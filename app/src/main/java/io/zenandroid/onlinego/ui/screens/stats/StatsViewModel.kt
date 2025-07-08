package io.zenandroid.onlinego.ui.screens.stats

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.HistoryItem
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.local.WinLossStats
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.FIVE_YEARS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.HUNDRED_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_MONTH
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_YEAR
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.THREE_MONTHS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.TWENTY_GAMES
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by alex on 05/11/2017.
 */
class StatsViewModel(
  private val restService: OGSRestService,
  private val getUserStatsUseCase: GetUserStatsUseCase,
  private val settingsRepository: SettingsRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  private val subscriptions = CompositeDisposable()
  private var stats: UserStats? = null
  private var currentFilter = ONE_MONTH
  private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
  private val graphByGames = settingsRepository.graphByGamesFlow.stateIn(
    scope = viewModelScope,
    initialValue = settingsRepository.cachedUserSettings.graphByGames,
    started = SharingStarted.WhileSubscribed(5_000)
  )

  val state: MutableStateFlow<StatsState> = MutableStateFlow(
    StatsState.Initial.copy(
      collapseTimeByGame = graphByGames.value,
    )
  )

  init {
    analyticsReportScreen("Stats")
    viewModelScope.launch(Dispatchers.IO) {
      val playerId = savedStateHandle.get<String>("playerId")?.toLong() ?: Util.getCurrentUserId()!!
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
        val playerId = savedStateHandle.get<String>("playerId")?.toLong() ?: Util.getCurrentUserId()!!
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
      it.copy(playerDetails = playerDetails)
    }
  }

  private fun fillPlayerStats(stats: UserStats) {
    this.stats = stats

    val highestRank = stats.highestRating?.let { formatRank(egfToRank(it.toDouble())) }
    val highestRankDate = stats.highestRatingTimestamp?.let { dateFormat.format(Date(it * 1000)) }
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
    val startDate = dateFormat.format(Date(stats.bestStreakStart * 1000))
    val endDate = dateFormat.format(Date(stats.bestStreakEnd * 1000))
    val currentStreak = if (last10Games.isEmpty()) "-" else {
      val lastGameWon = last10Games.last().won
      val count = last10Games.takeLastWhile { it.won == lastGameWon }.size
      val suffix =
        if (lastGameWon) "win${if (count != 1) "s" else ""}" else "loss${if (count != 1) "es" else ""}"
      "$count $suffix"
    }
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
        currentStreak = currentStreak,
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
      restService.getPlayerProfile(stats.mostFacedId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ mostFaced ->
          state.update {
            it.copy(
              mostFacedOpponent = mostFaced,
              mostFacedGameCount = stats.mostFacedGameCount,
              mostFacedWon = stats.mostFacedWon
            )
          }
        }, this::onError)
        .addToDisposable(subscriptions)
    }

    stats.highestWin?.let { winningGame ->
      restService.getPlayerProfile(winningGame.opponentId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ highestWin ->
          state.update {
            it.copy(
              highestWin = highestWin,
              winningGame = winningGame
            )
          }
        }, this::onError)
        .addToDisposable(subscriptions)
    } ?: run {
      //TODO
    }
  }

  private fun onError(t: Throwable) {
    Log.e("StatsPresenter", t.message, t)
    recordException(t)
  }

  override fun onCleared() {
    subscriptions.clear()
    super.onCleared()
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
    val chartData: List<Entry>,
    val playerDetails: OGSPlayer?,
    val highestRank: String?,
    val highestRankDate: String?,
    val lostCount: Int?,
    val wonCount: Int?,
    val gamesWonString: String?,
    val gamesLostString: String?,
    val last10Games: List<HistoryItem>?,
    val longestStreak: Int?,
    val currentStreak: String?,
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
        highestRank = null,
        highestRankDate = null,
        lostCount = null,
        wonCount = null,
        gamesWonString = null,
        gamesLostString = null,
        last10Games = null,
        longestStreak = null,
        currentStreak = null,
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


