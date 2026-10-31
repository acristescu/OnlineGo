package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.model.ogs.Warning
import io.zenandroid.onlinego.data.ogs.ConnectionStatus
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.httpErrorBody
import io.zenandroid.onlinego.data.ogs.httpStatusCode
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AutomatchRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.FinishedGamesRepository
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.GameSyncState
import io.zenandroid.onlinego.data.repositories.ServerNotificationsRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.auto
import io.zenandroid.onlinego.resources.disabled
import io.zenandroid.onlinego.resources.enabled
import io.zenandroid.onlinego.resources.error
import io.zenandroid.onlinego.resources.handicap
import io.zenandroid.onlinego.resources.login_to_play_online
import io.zenandroid.onlinego.resources.mygames_alert_bot_rejected_text
import io.zenandroid.onlinego.resources.mygames_alert_bot_rejected_title
import io.zenandroid.onlinego.resources.mygames_alert_game_unavailable
import io.zenandroid.onlinego.resources.mygames_alert_ogs_error_text
import io.zenandroid.onlinego.resources.mygames_alert_ogs_error_title
import io.zenandroid.onlinego.resources.mygames_alert_one_live_game
import io.zenandroid.onlinego.resources.mygames_alert_opponent_rejected_text
import io.zenandroid.onlinego.resources.mygames_alert_opponent_rejected_title
import io.zenandroid.onlinego.resources.mygames_detail_analysis
import io.zenandroid.onlinego.resources.mygames_detail_board_size
import io.zenandroid.onlinego.resources.mygames_detail_ranked
import io.zenandroid.onlinego.resources.mygames_detail_rules
import io.zenandroid.onlinego.resources.mygames_detail_speed
import io.zenandroid.onlinego.resources.mygames_greeting
import io.zenandroid.onlinego.resources.no
import io.zenandroid.onlinego.resources.welcome_to_sente_online_go
import io.zenandroid.onlinego.resources.yes
import io.zenandroid.onlinego.ui.screens.mygames.Action.AutomatchCancelled
import io.zenandroid.onlinego.ui.screens.mygames.Action.ChallengeAccepted
import io.zenandroid.onlinego.ui.screens.mygames.Action.ChallengeCancelled
import io.zenandroid.onlinego.ui.screens.mygames.Action.ChallengeDeclined
import io.zenandroid.onlinego.ui.screens.mygames.Action.ChallengeDialogDismissed
import io.zenandroid.onlinego.ui.screens.mygames.Action.ChallengeSeeDetails
import io.zenandroid.onlinego.ui.screens.mygames.Action.DismissAlertDialog
import io.zenandroid.onlinego.ui.screens.mygames.Action.DismissWhatsNewDialog
import io.zenandroid.onlinego.ui.screens.mygames.Action.GameNavigationConsumed
import io.zenandroid.onlinego.ui.screens.mygames.Action.GameSelected
import io.zenandroid.onlinego.ui.screens.mygames.Action.LoadMoreHistoricGames
import io.zenandroid.onlinego.ui.screens.mygames.Action.NewAutomatchSearch
import io.zenandroid.onlinego.ui.screens.mygames.Action.NewChallengeSearchClicked
import io.zenandroid.onlinego.ui.screens.mygames.Action.ViewResumed
import io.zenandroid.onlinego.ui.screens.mygames.Action.WarningAcknowledged
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.StringResource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class MyGamesViewModel(
  private val userSessionRepository: UserSessionRepository,
  private val finishedGamesRepository: FinishedGamesRepository,
  private val activeGamesRepository: ActiveGamesRepository,
  private val challengesRepository: ChallengesRepository,
  private val automatchRepository: AutomatchRepository,
  private val chatRepository: ChatRepository,
  private val notificationsRepository: ServerNotificationsRepository,
  private val tutorialsRepository: TutorialsRepository,
  private val analytics: Analytics,
  private val restService: OGSRestService,
  private val socketService: OGSWebSocketService,
  private val settingsRepository: SettingsRepository,
  private val whatsNewUtils: WhatsNewUtils,
) : ViewModel() {
  private val _state = MutableStateFlow(
    MyGamesState(
      userId = null,
      whatsNewDialogVisible = false,
    )
  )
  val state: StateFlow<MyGamesState> = _state
  private var loadOlderGamesJob: kotlinx.coroutines.Job? = null
  private val loggedInJobs = mutableListOf<kotlinx.coroutines.Job>()
  private var showRanks = false

  override fun onCleared() {
    loadOlderGamesJob?.cancel()
    super.onCleared()
  }

  init {
    viewModelScope.launch {
      userSessionRepository.loginStatus.collect { loggedInStatus ->
        when (loggedInStatus) {
          is LoginStatus.LoggedIn -> onLoggedIn(loggedInStatus.userId)
          LoginStatus.LoggedOut -> {
            _state.update {
              it.copy(
                userIsLoggedOut = true,
                userId = null,
                playOnlineEnabled = false,
                customGameEnabled = false,
                loginPromptVisible = true,
                headerMainTextResId = Res.string.welcome_to_sente_online_go,
                headerSubTextResId = Res.string.login_to_play_online,
              )
            }
          }
        }
      }
    }

    viewModelScope.launch {
      settingsRepository.showRanksFlow.collect {
        showRanks = it
      }
    }

    viewModelScope.launch {
      val shouldDisplay = whatsNewUtils.shouldDisplayDialog()
      _state.update {
        it.copy(
          whatsNewDialogVisible = shouldDisplay,
        )
      }
      whatsNewUtils.textShown()
    }

    viewModelScope.launch {
      socketService.connectionStatus.collect { status ->
        _state.update { it.copy(online = status == ConnectionStatus.Connected, connectionStatus = status) }
      }
    }
    viewModelScope.launch {
      activeGamesRepository.syncState.collect { syncState ->
        _state.update { it.copy(gameSyncState = syncState) }
      }
    }
  }

  private fun onLoggedIn(userId: Long) {
    loggedInJobs.forEach { it.cancel() }
    loggedInJobs.clear()

    loggedInJobs += viewModelScope.launch {
      try {
        val warning = restService.checkForWarnings()
        if (warning.id != null) {
          _state.update {
            it.copy(warning = warning)
          }
        }
      } catch (throwable: Throwable) {
        onError(throwable)
      }
    }

    loggedInJobs += viewModelScope.launch(Dispatchers.IO) {
      try {
        combine(
          activeGamesRepository.monitorActiveGames()
            .map { gamesList -> computePositions(gamesList) },
          finishedGamesRepository.getRecentlyFinishedGames()
            .map { gamesList -> computePositions(gamesList) },
          challengesRepository.monitorChallenges(),
          automatchRepository.automatchFlow,
        ) { activeGames, recentlyFinishedGames, challenges, automatches ->
          _state.update {
            var newState = it.copy(
              userIsLoggedOut = false,
              playOnlineEnabled = true,
              customGameEnabled = true,
              loginPromptVisible = false,
              userId = userId,
              headerMainTextResId = Res.string.mygames_greeting,
              headerMainTextArg = userSessionRepository.uiConfig?.user?.username,
              headerSubTextResId = null,
              userImageURL = userSessionRepository.uiConfig?.user?.icon,
              automatches = automatches,
              hasReceivedAutomatches = true,
              challenges = challenges,
              hasReceivedChallenges = true,
              recentGames = recentlyFinishedGames,
              hasReceivedRecentGames = true,
            )
            newState = setGames(activeGames, newState)
            newState
          }
        }.collect {}
      } catch (e: Exception) {
        onError(e)
      }
    }
    loggedInJobs += viewModelScope.launch(Dispatchers.IO) {
      try {
        activeGamesRepository.refreshActiveGames()
      } catch (e: Exception) {
        onError(e)
      }
    }
    loggedInJobs += viewModelScope.launch(Dispatchers.IO) {
      try {
        automatchRepository.gameStartFlow.collect { automatch ->
          automatch.game_id?.let { gameId ->
            try {
              val game = activeGamesRepository.refreshGameData(gameId)
              game?.let {
                withContext(Dispatchers.Main) { onGameStart(game) }
              }
            } catch (e: Exception) {
              onError(e)
            }
          }
        }
      } catch (e: Exception) {
        onError(e)
      }
    }
    loggedInJobs += viewModelScope.launch(Dispatchers.IO) {
      try {
        notificationsRepository.notificationsFlow().collect { onNotification(it) }
      } catch (e: Exception) {
        onError(e)
      }
    }

    onNeedMoreOlderGames(null)
  }

  private fun setGames(games: List<Game>, state: MyGamesState): MyGamesState {
    val myTurnList = mutableListOf<Game>()
    val opponentTurnList = mutableListOf<Game>()
    for (game in games) {
      val myTurn = when (game.phase) {
        Phase.PLAY -> game.playerToMoveId == state.userId
        Phase.STONE_REMOVAL -> {
          val myRemovedStones =
            if (state.userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
          game.removedStones != myRemovedStones
        }

        else -> false
      }

      if (myTurn) {
        myTurnList.add(game)
      } else {
        opponentTurnList.add(game)
      }
    }

    return state.copy(
      myTurnGames = myTurnList.sortedBy { timeLeftForCurrentPlayer(it) },
      opponentTurnGames = opponentTurnList,
      hasReceivedActiveGames = true,
    )
  }

  private fun onChallengeSeeDetails(challenge: Challenge) {
    val rank =
      formatRank(egfToRank(challenge.challenger?.rating), challenge.challenger?.deviation, true)
    val rating = challenge.challenger?.rating?.toInt()?.toString() ?: ""
    val status = ChallengeDialogStatus(
      challenge = challenge,
      imageURL = challenge.challenger?.icon,
      name = challenge.challenger?.username,
      rank = if (showRanks) "$rank ($rating)" else "",
      details = listOf(
        Res.string.mygames_detail_board_size to DetailValue.Literal("${challenge.width}x${challenge.height}"),
        Res.string.mygames_detail_speed to DetailValue.Literal("${challenge.speed?.replaceFirstChar { it.titlecase() }}"),
        Res.string.mygames_detail_ranked to DetailValue.Resource(if (challenge.ranked == true) Res.string.yes else Res.string.no),
        Res.string.mygames_detail_analysis to DetailValue.Resource(if (challenge.disabledAnalysis == true) Res.string.disabled else Res.string.enabled),
        Res.string.handicap to (challenge.handicap?.let { DetailValue.Literal(it.toString()) }
          ?: DetailValue.Resource(Res.string.auto)),
        Res.string.mygames_detail_rules to DetailValue.Literal("${challenge.rules?.replaceFirstChar { it.titlecase() }}"),
      ),
    )
    _state.update {
      it.copy(
        challengeDetailsStatus = status,
      )
    }
  }

  private fun onChallengeCancelled(challenge: Challenge) {
    analytics.logEvent("challenge_cancelled")
    viewModelScope.launch(Dispatchers.IO) {
      try {
        restService.declineChallenge(challenge.id)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }

  private fun onChallengeAccepted(challenge: Challenge) {
    analytics.logEvent("challenge_accepted")
    viewModelScope.launch(Dispatchers.IO) {
      try {
        restService.acceptChallenge(challenge.id)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }

  private fun onChallengeDeclined(challenge: Challenge) {
    analytics.logEvent("challenge_declined")
    viewModelScope.launch(Dispatchers.IO) {
      try {
        restService.declineChallenge(challenge.id)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }

  private fun onAutomatchCancelled(automatch: OGSAutomatch) {
    analytics.logEvent("new_game_cancelled")
    socketService.cancelAutomatch(automatch)
  }

  private fun onNotification(notification: JsonObject) {
    if (notification["type"]?.jsonPrimitive?.contentOrNull == "gameOfferRejected") {
      val message = notification["message"]?.jsonPrimitive?.contentOrNull
      if (notification["name"]?.jsonPrimitive?.contentOrNull == "Bot Match") {
        _state.update {
          it.copy(
            alertDialogTitleResId = Res.string.mygames_alert_bot_rejected_title,
            alertDialogTextResId = Res.string.mygames_alert_bot_rejected_text,
            alertDialogMessage = message
          )
        }
        analytics.logEvent("bot_refused_challenge")
        Logger.i("Bot refused challenge. $message", tag = "MyGamesViewModel")
      } else {
        _state.update {
          it.copy(
            alertDialogTitleResId = Res.string.mygames_alert_opponent_rejected_title,
            alertDialogTextResId = Res.string.mygames_alert_opponent_rejected_text,
            alertDialogMessage = message
          )
        }
      }
    }
  }

  private fun onError(t: Throwable) {
    if (t is CancellationException) {
      throw t
    }
    val statusCode = t.httpStatusCode
    if (statusCode != null) {
      if (statusCode in arrayOf(401, 403)) {
        CrashReporter.setCustomKey("AUTO_LOGOUT", Clock.System.now().toEpochMilliseconds())
        CrashReporter.recordException(Exception(t.httpErrorBody, t))
        CrashReporter.sendUnsentReports()
        userSessionRepository.logOut()
        _state.update {
          it.copy(
            userIsLoggedOut = true
          )
        }
      } else {
        CrashReporter.recordException(Exception(t.httpErrorBody, t))
      }
    } else {
      if (t is kotlinx.serialization.SerializationException) {
        _state.update {
          it.copy(
            alertDialogTitleResId = Res.string.mygames_alert_ogs_error_title,
            alertDialogTextResId = Res.string.mygames_alert_ogs_error_text
          )
        }
      }
      CrashReporter.recordException(t)
    }

    Logger.e(t.message.orEmpty(), t, "MyGamesViewModel")
  }

  fun onAction(action: Action) {
    Logger.i(action::class.qualifiedName.orEmpty(), tag = "MyGamesViewModel")
    when (action) {
      is ChallengeAccepted -> onChallengeAccepted(action.challenge)
      is ChallengeSeeDetails -> onChallengeSeeDetails(action.challenge)
      is ChallengeCancelled -> onChallengeCancelled(action.challenge)
      is ChallengeDeclined -> onChallengeDeclined(action.challenge)
      is AutomatchCancelled -> onAutomatchCancelled(action.automatch)
      is LoadMoreHistoricGames -> onNeedMoreOlderGames(action.game)
      is DismissWhatsNewDialog -> onDismissWhatsNewDialog()
      ChallengeDialogDismissed -> _state.update { it.copy(challengeDetailsStatus = null) }
      DismissAlertDialog -> onDismissAlertDialog()
      GameNavigationConsumed -> onGameNavigationConsumed()
      ViewResumed -> onViewResumed()
      WarningAcknowledged -> onWarningAcknowledged()
      is NewChallengeSearchClicked -> onNewChallengeSearchClicked(action.challenge)
      is GameSelected -> {
        analytics.logEvent("game_selected")
        val game = action.game
        if (game.id == 0L) {
          _state.update {
            it.copy(
              alertDialogTitleResId = Res.string.error,
              alertDialogTextResId = Res.string.mygames_alert_game_unavailable
            )
          }
        } else {
          _state.update {
            it.copy(
              gameNavigationPending = game
            )
          }
        }
      }

      is NewAutomatchSearch -> {
        analytics.logEvent("new_game_search")
        if ((action.speeds.contains(Speed.LIVE) || action.speeds.contains(Speed.RAPID) || action.speeds.contains(
            Speed.BLITZ
          )) && automatchRepository.automatchFlow.value.find { it.liveOrBlitzOrRapid } != null
        ) {
          _state.update {
            it.copy(
              alertDialogTitleResId = Res.string.error,
              alertDialogTextResId = Res.string.mygames_alert_one_live_game
            )
          }
        } else {
          socketService.startAutomatch(action.sizes, action.speeds)
        }
      }
    }
  }

  private fun onNewChallengeSearchClicked(challengeParams: ChallengeParams) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        restService.challengeBot(challengeParams)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }

  private fun onWarningAcknowledged() {
    _state.update {
      it.warning?.let {
        viewModelScope.launch {
          try {
            restService.acknowledgeWarning(it)
          } catch (throwable: Throwable) {
            onError(throwable)
          }
        }
      }
      it.copy(warning = null)
    }
  }

  private fun onViewResumed() {
    chatRepository.fetchRecentChatMessages()
  }

  private fun onGameNavigationConsumed() {
    _state.update {
      it.copy(
        gameNavigationPending = null
      )
    }
  }

  private fun onDismissAlertDialog() {
    _state.update {
      it.copy(
        alertDialogTextResId = null,
        alertDialogTitleResId = null,
        alertDialogMessage = null
      )
    }
  }

  private fun onDismissWhatsNewDialog() {
    _state.update {
      it.copy(
        whatsNewDialogVisible = false
      )
    }
  }

  private fun onGameStart(game: Game) {
    _state.update {
      it.copy(
        gameNavigationPending = game
      )
    }
  }

  private fun onNeedMoreOlderGames(lastGame: Game?) {
    loadOlderGamesJob?.cancel()
    loadOlderGamesJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        finishedGamesRepository.getHistoricGames(lastGame?.ended)
          .distinctUntilChanged()
          .onEach { result ->
            _state.update {
              it.copy(
                loadedAllHistoricGames = result.loadedLastPage
              )
            }
          }
          .map { it.games }
          .map { games -> computePositions(games) }
          .collect { games -> withContext(Dispatchers.Main) { onHistoricGames(games) } }
      } catch (e: Exception) {
        onError(e)
      }
    }
  }

  private suspend fun computePositions(games: List<Game>): List<Game> =
    coroutineScope {
      games.map { game ->
        async(Dispatchers.Default) {
          game.apply { position = RulesManager.replay(this, computeTerritory = false) }
        }
      }.awaitAll()
    }

  private fun onHistoricGames(games: List<Game>) {
    _state.update {
      val existingGames = it.historicGames
      val existingGameIds = existingGames.map { it.id }.toSet()
      val updatedExistingGames = existingGames.map { existing ->
        games.find { it.id == existing.id } ?: existing
      }
      val newGames = games.filter { candidate -> candidate.id !in existingGameIds }
      it.copy(
        historicGames = updatedExistingGames + newGames,
        hasReceivedHistoricGames = true,
      )
    }
  }

}

@Immutable
data class MyGamesState(
  val myTurnGames: List<Game> = emptyList(),
  val opponentTurnGames: List<Game> = emptyList(),
  val recentGames: List<Game> = emptyList(),
  val challenges: List<Challenge> = emptyList(),
  val automatches: List<OGSAutomatch> = emptyList(),
  val historicGames: List<Game> = emptyList(),
  val loadedAllHistoricGames: Boolean = false,
  val userId: Long?,
  val userIsLoggedOut: Boolean = false,
  val alertDialogTitleResId: StringResource? = null,
  val alertDialogTextResId: StringResource? = null,
  val alertDialogMessage: String? = null,
  val gameNavigationPending: Game? = null,
  val whatsNewDialogVisible: Boolean = false,
  val userImageURL: String? = null,
  val headerMainTextResId: StringResource? = null,
  val headerMainTextArg: String? = null,
  val headerSubTextResId: StringResource? = null,
  val tutorialPercentage: Int? = 100,
  val tutorialVisible: Boolean = false,
  val tutorialTitle: String? = null,
  val online: Boolean = true,
  val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
  val gameSyncState: GameSyncState = GameSyncState(),
  val challengeDetailsStatus: ChallengeDialogStatus? = null,
  val warning: Warning? = null,
  val hasReceivedActiveGames: Boolean = false,
  val hasReceivedRecentGames: Boolean = false,
  val hasReceivedChallenges: Boolean = false,
  val hasReceivedAutomatches: Boolean = false,
  val hasReceivedHistoricGames: Boolean = false,
  val playOnlineEnabled: Boolean = true,
  val customGameEnabled: Boolean = true,
  val loginPromptVisible: Boolean = false,
)


sealed interface Action {
  data object DismissWhatsNewDialog : Action
  data object DismissAlertDialog : Action
  data object GameNavigationConsumed : Action
  data class GameSelected(val game: Game) : Action
  data class ChallengeCancelled(val challenge: Challenge) : Action
  data class ChallengeSeeDetails(val challenge: Challenge) : Action
  data class ChallengeAccepted(val challenge: Challenge) : Action
  data object ChallengeDialogDismissed : Action
  data class ChallengeDeclined(val challenge: Challenge) : Action
  data class AutomatchCancelled(val automatch: OGSAutomatch) : Action
  data class LoadMoreHistoricGames(val game: Game?) : Action
  data object ViewResumed : Action
  data object WarningAcknowledged : Action
  data class NewChallengeSearchClicked(val challenge: ChallengeParams) : Action
  data class NewAutomatchSearch(val speeds: List<Speed>, val sizes: List<Size>) : Action
}

@Immutable
data class ChallengeDialogStatus(
  val challenge: Challenge,
  val imageURL: String?,
  val name: String?,
  val rank: String,
  val details: List<Pair<StringResource, DetailValue>>
)

/**
 * A challenge detail value, which is either a literal (server-provided data such as a board size)
 * or a string resource that has to be resolved against the current locale.
 */
@Immutable
sealed interface DetailValue {
  data class Literal(val text: String) : DetailValue
  data class Resource(val resId: StringResource) : DetailValue
}
