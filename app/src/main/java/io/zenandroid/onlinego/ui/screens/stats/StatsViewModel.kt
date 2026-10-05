package io.zenandroid.onlinego.ui.screens.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.local.HistoryItem
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.local.WinLossStats
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.FIVE_YEARS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.HUNDRED_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_MONTH
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_YEAR
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.THREE_MONTHS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.TWENTY_GAMES
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Created by alex on 05/11/2017.
 */
class StatsViewModel(
  private val restService: OGSRestService,
  private val getUserStatsUseCase: GetUserStatsUseCase,
  private val settingsRepository: SettingsRepository,
  private val savedStateHandle: SavedStateHandle,
  private val userSessionRepository: UserSessionRepository,
) : ViewModel() {

  private var stats: UserStats? = null
  private var currentFilter = ONE_MONTH
  private val graphByGames = settingsRepository.graphByGamesFlow.stateIn(
    scope = viewModelScope,
    initialValue = settingsRepository.cachedUserSettings.graphByGames,
    started = SharingStarted.WhileSubscribed(5_000)
  )

  private val requestedPlayerId = savedStateHandle.get<String>("playerId")?.toLong()

  /**
   * Viewing your own stats is a bottom bar tab, so it is entered often and the view model is
   * rebuilt each time. The session already knows the avatar, so seeding it here means the header
   * draws on the first frame instead of waiting out [OGSRestService.getPlayerProfileAsync].
   */
  val state: MutableStateFlow<StatsState> = MutableStateFlow(
    StatsState.Initial.copy(
      collapseTimeByGame = graphByGames.value,
      avatarURL = if (requestedPlayerId == null) userSessionRepository.uiConfig?.user?.icon else null,
    )
  )

  init {
    viewModelScope.launch(Dispatchers.IO) {
      val userId = userSessionRepository.userId.filterNotNull().first()
      val playerId = requestedPlayerId ?: userId
      val result = getUserStatsUseCase.getPlayerStatsWithSizesAsync(playerId)
      result.fold(
        onSuccess = ::fillPlayerStats,
        onFailure = ::onError
      )
    }

    viewModelScope.launch {
      graphByGames.collect {
        state.update {
          it.copy(
            collapseTimeByGame = it.collapseTimeByGame
          )
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val playerId = requestedPlayerId
          ?: userSessionRepository.userId.filterNotNull().first()
        fillPlayerDetails(restService.getPlayerProfileAsync(playerId))
      } catch (t: Throwable) {
        onError(t)
      }
    }
  }

  fun onGraphChanged() {
    stats?.let {
      viewModelScope.launch {
        settingsRepository.setGraphByGames(!graphByGames.value)
      }
      state.update {
        it.copy(
          collapseTimeByGame = it.collapseTimeByGame?.not()
        )
      }
    }
  }

  fun onFilterChanged(filter: Filter) {
    currentFilter = filter
    stats?.let { stats ->
      state.update {
        it.copy(
          chartData = when (filter) {
            ONE_MONTH -> stats.chartData1M
            THREE_MONTHS -> stats.chartData3M
            ONE_YEAR -> stats.chartData1Y
            FIVE_YEARS -> stats.chartData5Y
            ALL -> stats.chartDataAll
            TWENTY_GAMES -> stats.chartData20G
            HUNDRED_GAMES -> stats.chartData100G
            ALL_GAMES -> stats.chartDataAllG
          },
          filter = filter,
        )
      }
    }
  }

  private fun fillPlayerDetails(playerDetails: OGSPlayer) {
    state.update {
      it.copy(playerDetails = playerDetails, avatarURL = playerDetails.icon ?: it.avatarURL)
    }
  }

  private fun fillPlayerStats(stats: UserStats) {
    this.stats = stats

    val highestRank = stats.highestRating?.let { formatRank(egfToRank(it.toDouble())) }
    val highestRankDate = stats.highestRatingTimestamp?.let { formatDate(it) }
    val chartData = when (currentFilter) {
      ONE_MONTH -> stats.chartData1M
      THREE_MONTHS -> stats.chartData3M
      ONE_YEAR -> stats.chartData1Y
      FIVE_YEARS -> stats.chartData5Y
      ALL -> stats.chartDataAll
      TWENTY_GAMES -> stats.chartData20G
      HUNDRED_GAMES -> stats.chartData100G
      ALL_GAMES -> stats.chartDataAllG
    }
    val lostCount = stats.lostCount
    val wonCount = stats.wonCount
    val gamesWonPercent = (wonCount * 100 / (wonCount + lostCount).toFloat())
    val gamesLostPercent = 100 - gamesWonPercent

    val gamesWonString = String.format("%.1f", gamesWonPercent)
    val gamesLostString = String.format("%.1f", gamesLostPercent)
    val last10Games = stats.last10Games
    val longestStreak = stats.bestStreak
    val startDate = formatDate(stats.bestStreakStart)
    val endDate = formatDate(stats.bestStreakEnd)
    val lastGameWon = last10Games.lastOrNull()?.won
    val currentStreakCount =
      if (last10Games.isEmpty()) 0
      else last10Games.takeLastWhile { it.won == lastGameWon }.size
    val recentWins = last10Games.count { it.won }
    val recentLosses = last10Games.count { !it.won }

    state.update {
      it.copy(
        highestRank = highestRank,
        highestRankDate = highestRankDate,
        chartData = chartData,
        lostCount = lostCount,
        wonCount = wonCount,
        gamesWonString = gamesWonString,
        gamesLostString = gamesLostString,
        last10Games = last10Games,
        longestStreak = longestStreak,
        currentStreakCount = currentStreakCount,
        currentStreakWon = lastGameWon,
        recentResults = "$recentWins - $recentLosses",
        startDate = startDate,
        endDate = endDate,
        collapseTimeByGame = graphByGames.value,
        allGames = stats.allGames,
        smallBoard = stats.smallBoard,
        mediumBoard = stats.mediumBoard,
        largeBoard = stats.largeBoard,
        blitz = stats.blitz,
        live = stats.live,
        asWhite = stats.asWhite,
        asBlack = stats.asBlack,
        correspondence = stats.correspondence,
      )
    }

    if (stats.mostFacedId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        try {
          val mostFaced = restService.getPlayerProfile(stats.mostFacedId)
          state.update {
            it.copy(
              mostFacedOpponent = mostFaced,
              mostFacedGameCount = stats.mostFacedGameCount,
              mostFacedWon = stats.mostFacedWon
            )
          }
        } catch (e: Exception) {
          onError(e)
        }
      }
    }

    stats.highestWin?.let { winningGame ->
      viewModelScope.launch(Dispatchers.IO) {
        try {
          val highestWin = restService.getPlayerProfile(winningGame.opponentId)
          state.update {
            it.copy(
              highestWin = highestWin,
              winningGame = winningGame
            )
          }
        } catch (e: Exception) {
          onError(e)
        }
      }
    } ?: run {
      //TODO
    }
  }

  private fun onError(t: Throwable) {
    Logger.e(t.message.orEmpty(), t, "StatsPresenter")
    CrashReporter.recordException(t)
  }

  enum class Filter {
    ONE_MONTH,
    THREE_MONTHS,
    ONE_YEAR,
    FIVE_YEARS,
    ALL,
    TWENTY_GAMES,
    HUNDRED_GAMES,
    ALL_GAMES
  }

  @Immutable
  data class StatsState(
    val chartData: List<Pair<Float, Float>>,
    val playerDetails: OGSPlayer?,
    val avatarURL: String?,
    val highestRank: String?,
    val highestRankDate: String?,
    val lostCount: Int?,
    val wonCount: Int?,
    val gamesWonString: String?,
    val gamesLostString: String?,
    val last10Games: List<HistoryItem>?,
    val longestStreak: Int?,
    val currentStreakCount: Int?,
    val currentStreakWon: Boolean?,
    val recentResults: String?,
    val startDate: String?,
    val endDate: String?,
    val mostFacedOpponent: OGSPlayer?,
    val mostFacedGameCount: Int?,
    val mostFacedWon: Int?,
    val highestWin: OGSPlayer?,
    val winningGame: HistoryItem?,
    val collapseTimeByGame: Boolean?,
    val filter: Filter = ONE_MONTH,
    val allGames: WinLossStats?,
    val smallBoard: WinLossStats?,
    val mediumBoard: WinLossStats?,
    val largeBoard: WinLossStats?,
    val blitz: WinLossStats?,
    val live: WinLossStats?,
    val asWhite: WinLossStats?,
    val asBlack: WinLossStats?,
    val correspondence: WinLossStats?,
  ) {
    companion object {
      val Initial = StatsState(
        chartData = emptyList(),
        playerDetails = null,
        avatarURL = null,
        highestRank = null,
        highestRankDate = null,
        lostCount = null,
        wonCount = null,
        gamesWonString = null,
        gamesLostString = null,
        last10Games = null,
        longestStreak = null,
        currentStreakCount = null,
        currentStreakWon = null,
        recentResults = null,
        startDate = null,
        endDate = null,
        mostFacedOpponent = null,
        mostFacedGameCount = null,
        mostFacedWon = null,
        highestWin = null,
        winningGame = null,
        collapseTimeByGame = null,
        allGames = null,
        smallBoard = null,
        mediumBoard = null,
        largeBoard = null,
        blitz = null,
        live = null,
        asWhite = null,
        asBlack = null,
        correspondence = null,
      )
    }
  }
}

private val dateFormat = LocalDate.Format {
  monthName(MonthNames.ENGLISH_ABBREVIATED)
  char(' ')
  day(padding = Padding.NONE)
  chars(", ")
  year()
}

private fun formatDate(secondsSinceEpoch: Long): String =
  Instant.fromEpochSeconds(secondsSinceEpoch)
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .date
    .format(dateFormat)
