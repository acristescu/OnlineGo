package io.zenandroid.onlinego.ui.screens.mygames

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.model.ogs.Warning
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AutomatchRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.FinishedGamesRepository
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.ServerNotificationsRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
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
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class MyGamesViewModel(
  private val userSessionRepository: UserSessionRepository,
  private val finishedGamesRepository: FinishedGamesRepository,
  private val activeGamesRepository: ActiveGamesRepository,
  private val challengesRepository: ChallengesRepository,
  private val automatchRepository: AutomatchRepository,
  private val chatRepository: ChatRepository,
  private val notificationsRepository: ServerNotificationsRepository,
  private val tutorialsRepository: TutorialsRepository,
  private val analytics: FirebaseAnalytics,
  private val restService: OGSRestService,
  private val socketService: OGSWebSocketService,
  private val settingsRepository: SettingsRepository
) : ViewModel() {
  private val _state = MutableStateFlow(
    MyGamesState(
      userId = null,
      whatsNewDialogVisible = false,
      headerMainText = "",
    )
  )
  val state: StateFlow<MyGamesState> = _state
  private val subscriptions = CompositeDisposable()
  private var loadOlderGamesSubscription: Disposable? = null
  private var showRanks = false

  override fun onCleared() {
    subscriptions.clear()
    loadOlderGamesSubscription?.dispose()
    super.onCleared()
  }

  init {
    userSessionRepository.loggedInObservable
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { loggedInStatus ->
        when(loggedInStatus) {
          is LoginStatus.LoggedIn -> onLoggedIn(loggedInStatus.userId)
          LoginStatus.LoggedOut -> {
            _state.update {
              it.copy(
                userIsLoggedOut = true,
                userId = null,
                playOnlineEnabled = false,
                customGameEnabled = false,
                loginPromptVisible = true,
                headerMainText = "Welcome to Sente Online Go!",
                headerSubText = "Please log in to OGS to play online.",
              )
            }
          }
        }
      }.addToDisposable(subscriptions)

    viewModelScope.launch {
      settingsRepository.showRanksFlow.collect {
        showRanks = it
      }
    }

    viewModelScope.launch {
      val shouldDisplay = WhatsNewUtils.shouldDisplayDialog(OnlineGoApplication.instance)
      _state.update {
        it.copy(
          whatsNewDialogVisible = shouldDisplay,
        )
      }
      WhatsNewUtils.textShown(OnlineGoApplication.instance)
    }

    viewModelScope.launch {
      socketService.connectionState.collect { online ->
        _state.update { it.copy(online = online) }
      }
    }
  }

  private fun onLoggedIn(userId: Long) {
    _state.update {
      it.copy(
        userIsLoggedOut = false,
        playOnlineEnabled = true,
        customGameEnabled = true,
        loginPromptVisible = false,
        userId = userId,
        headerMainText = "Hi ${userSessionRepository.uiConfig?.user?.username},",
        userImageURL = userSessionRepository.uiConfig?.user?.icon,
      )
    }

    viewModelScope.launch {
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

    activeGamesRepository.monitorActiveGames()
      .subscribeOn(Schedulers.io())
      .map(this::computePositions)
      .subscribeOn(Schedulers.computation())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::setGames, this::onError)
      .addToDisposable(subscriptions)
    activeGamesRepository.refreshActiveGames()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({}, this::onError)
      .addToDisposable(subscriptions)
    finishedGamesRepository.getRecentlyFinishedGames()
      .subscribeOn(Schedulers.io())
      .map(this::computePositions)
      .subscribeOn(Schedulers.computation())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::setRecentGames, this::onError)
      .addToDisposable(subscriptions)
    challengesRepository.monitorChallenges()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::setChallenges, this::onError)
      .addToDisposable(subscriptions)
    automatchRepository.automatchObservable
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::setAutomatches, this::onError)
      .addToDisposable(subscriptions)
    automatchRepository.gameStartObservable
      .flatMapMaybe {
        it.game_id?.let { activeGamesRepository.getGameSingle(it).toMaybe() } ?: Maybe.empty()
      }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onGameStart, this::onError)
      .addToDisposable(subscriptions)
    notificationsRepository.notificationsObservable()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::onNotification, this::onError)
      .addToDisposable(subscriptions)

    onNeedMoreOlderGames(null)
  }

  private fun setGames(games: List<Game>) {
    val myTurnList = mutableListOf<Game>()
    val opponentTurnList = mutableListOf<Game>()
    for (game in games) {
      val myTurn = when (game.phase) {
        Phase.PLAY -> game.playerToMoveId == _state.value.userId
        Phase.STONE_REMOVAL -> {
          val myRemovedStones =
            if (_state.value.userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
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

    _state.update {
      it.copy(
        myTurnGames = myTurnList.sortedBy { timeLeftForCurrentPlayer(it) },
        opponentTurnGames = opponentTurnList,
        headerSubText = determineText(myTurnList, opponentTurnList),
        hasReceivedActiveGames = true,
      )
    }
  }

  private fun determineText(myTurnGames: List<Game>, opponentTurnGames: List<Game>): String {
    if (myTurnGames.isNotEmpty()) {
      return "It's your turn in ${myTurnGames.size} games."
    }
    if (opponentTurnGames.isNotEmpty()) {
      return "You have ${opponentTurnGames.size} active games."
    }
    return "You have no active games. How about starting one?"
  }

  private fun setRecentGames(games: List<Game>) {
    _state.update {
      it.copy(
        recentGames = games,
        hasReceivedRecentGames = true,
      )
    }
  }

  private fun setChallenges(challenges: List<Challenge>) {
    _state.update {
      it.copy(
        challenges = challenges,
        hasReceivedChallenges = true,
      )
    }
  }


  private fun setAutomatches(automatches: List<OGSAutomatch>) {
    _state.update {
      it.copy(
        automatches = automatches,
        hasReceivedAutomatches = true,
      )
    }
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
        "Board Size" to "${challenge.width}x${challenge.height}",
        "Speed" to "${challenge.speed?.capitalize(Locale.UK)}",
        "Ranked" to if (challenge.ranked == true) "Yes" else "No",
        "Analysis" to if (challenge.disabledAnalysis == true) "Disabled" else "Enabled",
        "Handicap" to "${challenge.handicap ?: "Auto"}",
        "Rules" to "${challenge.rules?.capitalize(Locale.UK)}",
      ),
    )
    _state.update {
      it.copy(
        challengeDetailsStatus = status,
      )
    }
  }

  private fun onChallengeCancelled(challenge: Challenge) {
    analytics.logEvent("challenge_cancelled", null)
    restService.declineChallenge(challenge.id)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({}, this::onError)
      .addToDisposable(subscriptions)
  }

  private fun onChallengeAccepted(challenge: Challenge) {
    analytics.logEvent("challenge_accepted", null)
    restService.acceptChallenge(challenge.id)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({}, this::onError)
      .addToDisposable(subscriptions)
  }

  private fun onChallengeDeclined(challenge: Challenge) {
    analytics.logEvent("challenge_declined", null)
    restService.declineChallenge(challenge.id)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({}, this::onError)
      .addToDisposable(subscriptions)
  }

  private fun onAutomatchCancelled(automatch: OGSAutomatch) {
    analytics.logEvent("new_game_cancelled", null)
    socketService.cancelAutomatch(automatch)
  }

  private fun onNotification(notification: JSONObject) {
    if (notification["type"] == "gameOfferRejected") {
      notificationsRepository.acknowledgeNotification(notification)
      val message =
        if (notification.has("message") && notification["message"].toString() != "null") "Message is:\n\n${notification["message"]}" else ""
      if (notification["name"].toString() == "Bot Match") {
        _state.update {
          it.copy(
            alertDialogTitle = "Bot rejected challenge",
            alertDialogText = "This might happen because the opponent's maintainer has set some conditions on the challenge parameters. $message"
          )
        }
        analytics.logEvent("bot_refused_challenge", null)
        FirebaseCrashlytics.getInstance().log("Bot refused challenge. $message")
      } else {
        _state.update {
          it.copy(
            alertDialogTitle = "Opponent rejected challenge",
            alertDialogText = "You may try again or otherwise contact the opponent to clarify his/her reasons for the rejection. $message"
          )
        }
      }
    }
  }

  private fun onError(t: Throwable) {
    if (t is retrofit2.HttpException) {
      if (t.code() in arrayOf(401, 403)) {
        FirebaseCrashlytics.getInstance().setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
        recordException(Exception(t.response()?.errorBody()?.string(), t))
        FirebaseCrashlytics.getInstance().sendUnsentReports()
        userSessionRepository.logOut()
        _state.update {
          it.copy(
            userIsLoggedOut = true
          )
        }
      } else {
        recordException(Exception(t.response()?.errorBody()?.string(), t))
      }
    } else {
      if (t is com.squareup.moshi.JsonDataException) {
        _state.update {
          it.copy(
            alertDialogTitle = "OGS Error",
            alertDialogText = "An error occurred white talking to the OGS Server. This usually means the website devs have changed something in the API. Please report this error as the app will probably not work until we adapt to this change."
          )
        }
      }
      recordException(t)
    }

    Log.e("MyGamesViewModel", t.message, t)
  }

  fun onAction(action: Action) {
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
        analytics.logEvent("game_selected", null)
        val game = action.game
        if (game.id == 0L) {
          _state.update {
            it.copy(
              alertDialogTitle = "Error",
              alertDialogText = "This game is not available."
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
        analytics.logEvent("new_game_search", null)
        if ((action.speeds.contains(Speed.LIVE) || action.speeds.contains(Speed.RAPID) || action.speeds.contains(
            Speed.BLITZ
          )) && automatchRepository.automatches.find { it.liveOrBlitzOrRapid } != null
        ) {
          _state.update {
            it.copy(
              alertDialogTitle = "Error",
              alertDialogText = "Can only search for one live, rapid or blitz game at a time."
            )
          }
        } else {
          socketService.startAutomatch(action.sizes, action.speeds)
        }
      }
    }
  }

  private fun onNewChallengeSearchClicked(challengeParams: ChallengeParams) {
    restService.challengeBot(challengeParams)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({}, this::onError)
      .addToDisposable(subscriptions)
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
        alertDialogText = null,
        alertDialogTitle = null
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
    loadOlderGamesSubscription?.dispose()
    loadOlderGamesSubscription =
      finishedGamesRepository.getHistoricGames(lastGame?.ended)
        .observeOn(AndroidSchedulers.mainThread())
        .distinctUntilChanged()
        .doOnNext { result ->
          _state.update {
            it.copy(
              loadingHistoricGames = result.loading,
              loadedAllHistoricGames = result.loadedLastPage
            )
          }
        }
        .map { it.games }
        .map(this::computePositions)
        .subscribe(this::onHistoricGames, this::onError)
    loadOlderGamesSubscription?.addToDisposable(subscriptions)
  }

  private fun computePositions(games: List<Game>): List<Game> =
    games.onEach { it.position = RulesManager.replay(it, computeTerritory = false) }

  private fun onHistoricGames(games: List<Game>) {
    _state.update {
      val existingGames = it.historicGames
      val newGames =
        games.filter { candidate -> existingGames.find { candidate.id == it.id } == null }
      it.copy(
        historicGames = existingGames + newGames,
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
  val loadingHistoricGames: Boolean = false,
  val loadedAllHistoricGames: Boolean = false,
  val userId: Long?,
  val userIsLoggedOut: Boolean = false,
  val alertDialogTitle: String? = null,
  val alertDialogText: String? = null,
  val gameNavigationPending: Game? = null,
  val whatsNewDialogVisible: Boolean = false,
  val userImageURL: String? = null,
  val headerMainText: String,
  val headerSubText: String? = null,
  val tutorialPercentage: Int? = 100,
  val tutorialVisible: Boolean = false,
  val tutorialTitle: String? = null,
  val online: Boolean = true,
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
  val details: List<Pair<String, String>>
)
