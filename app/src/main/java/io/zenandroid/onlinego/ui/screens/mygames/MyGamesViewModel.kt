package io.zenandroid.onlinego.ui.screens.mygames

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.annotation.concurrent.Immutable

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
    private val _state = MutableStateFlow(MyGamesState(
        userId = userSessionRepository.userId ?: 0,
        whatsNewDialogVisible = WhatsNewUtils.shouldDisplayDialog,
        headerMainText = "Hi ${userSessionRepository.uiConfig?.user?.username},",
        userImageURL = userSessionRepository.uiConfig?.user?.icon,
        boardTheme = settingsRepository.boardTheme,
    ))
    val state: StateFlow<MyGamesState> = _state
    private val subscriptions = CompositeDisposable()
    private var loadOlderGamesSubscription: Disposable? = null

    override fun onCleared() {
        subscriptions.clear()
        loadOlderGamesSubscription?.dispose()
        super.onCleared()
    }

    init {
        activeGamesRepository.monitorActiveGames()
            .subscribeOn(Schedulers.io())
            .map(this::computePositions)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::setGames, this::onError)
            .addToDisposable(subscriptions)
        activeGamesRepository.refreshActiveGames()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
        finishedGamesRepository.getRecentlyFinishedGames()
            .subscribeOn(Schedulers.io())
            .map(this::computePositions)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::setRecentGames, this::onError)
            .addToDisposable(subscriptions)
        challengesRepository.monitorChallenges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::setChallenges, this::onError)
            .addToDisposable(subscriptions)
        automatchRepository.automatchObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::setAutomatches, this::onError)
            .addToDisposable(subscriptions)
        automatchRepository.gameStartObservable
            .flatMapMaybe { it.game_id?.let { activeGamesRepository.getGameSingle(it).toMaybe() } ?: Maybe.empty() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::onGameStart, this::onError)
            .addToDisposable(subscriptions)
        notificationsRepository.notificationsObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe(this::onNotification, this::onError)
            .addToDisposable(subscriptions)

        onNeedMoreOlderGames(null)
    }

    private fun setGames(games: List<Game>) {
        val myTurnList = mutableListOf<Game>()
        val opponentTurnList = mutableListOf<Game>()
        for(game in games) {
            val myTurn = when (game.phase) {
                Phase.PLAY -> game.playerToMoveId == userSessionRepository.userId
                Phase.STONE_REMOVAL -> {
                    val myRemovedStones = if(userSessionRepository.userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                    game.removedStones != myRemovedStones
                }
                else -> false
            }

            if(myTurn) {
                myTurnList.add(game)
            } else {
                opponentTurnList.add(game)
            }
        }

        _state.value = _state.value.copy(
            myTurnGames = myTurnList.sortedBy { timeLeftForCurrentPlayer(it) },
            opponentTurnGames = opponentTurnList,
            headerSubText = determineText(myTurnList, opponentTurnList)
        )
    }

    private fun determineText(myTurnGames: List<Game>, opponentTurnGames: List<Game>): String {
        if(myTurnGames.isNotEmpty()) {
            return "It's your turn in ${myTurnGames.size} games."
        }
        if(opponentTurnGames.isNotEmpty()) {
            return "You have ${opponentTurnGames.size} active games."
        }
        return "You have no active games. How about starting one?"
    }

    private fun setRecentGames(games: List<Game>) {
        _state.value = _state.value.copy(
            recentGames = games
        )
    }

    private fun setChallenges(challenges: List<Challenge>) {
        _state.value = _state.value.copy(
            challenges = challenges
        )
    }


    private fun setAutomatches(automatches: List<OGSAutomatch>) {
        _state.value = _state.value.copy(
            automatches = automatches
        )
    }


    private fun onChallengeCancelled(challenge: Challenge) {
        analytics.logEvent("challenge_cancelled", null)
        restService.declineChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun onChallengeAccepted(challenge: Challenge) {
        analytics.logEvent("challenge_accepted", null)
        restService.acceptChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun onChallengeDeclined(challenge: Challenge) {
        analytics.logEvent("challenge_declined", null)
        restService.declineChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun onAutomatchCancelled(automatch: OGSAutomatch) {
        analytics.logEvent("new_game_cancelled", null)
        socketService.cancelAutomatch(automatch)
    }

    private fun onNotification(notification: JSONObject) {
        if(notification["type"] == "gameOfferRejected") {
            notificationsRepository.acknowledgeNotification(notification)
            val message = if(notification.has("message") && notification["message"].toString() != "null") "Message is:\n\n${notification["message"]}" else ""
            if (notification["name"].toString() == "Bot Match") {
                _state.value = _state.value.copy(
                    alertDialogTitle = "Bot rejected challenge",
                    alertDialogText = "This might happen because the opponent's maintainer has set some conditions on the challenge parameters. $message"
                )
                analytics.logEvent("bot_refused_challenge", null)
                FirebaseCrashlytics.getInstance().log("Bot refused challenge. $message")
            } else {
                _state.value = _state.value.copy(
                    alertDialogTitle = "Opponent rejected challenge",
                    alertDialogText = "You may try again or otherwise contact the opponent to clarify his/her reasons for the rejection. $message"
                )
            }
        }
    }

    private fun onError(t: Throwable) {
        if(t is retrofit2.HttpException) {
            if(t.code() in arrayOf(401, 403)) {
                FirebaseCrashlytics.getInstance().setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
                FirebaseCrashlytics.getInstance().recordException(Exception(t.response()?.errorBody()?.string(), t))
                FirebaseCrashlytics.getInstance().sendUnsentReports()
                userSessionRepository.logOut()
                _state.value = _state.value.copy(
                    userIsLoggedOut = true
                )
            } else {
                FirebaseCrashlytics.getInstance().recordException(Exception(t.response()?.errorBody()?.string(), t))
            }
        } else {
            if(t is com.squareup.moshi.JsonDataException) {
                _state.value = _state.value.copy(
                    alertDialogTitle = "OGS Error",
                    alertDialogText = "An error occurred white talking to the OGS Server. This usually means the website devs have changed something in the API. Please report this error as the app will probably not work until we adapt to this change."
                )
            }
            FirebaseCrashlytics.getInstance().recordException(t)
        }

        Log.e("MyGamesViewModel", t.message, t)
    }

    fun onAction(action: Action) {
        when(action) {
            is Action.ChallengeAccepted -> onChallengeAccepted(action.challenge)
            is Action.ChallengeCancelled -> onChallengeCancelled(action.challenge)
            is Action.ChallengeDeclined -> onChallengeDeclined(action.challenge)
            is Action.AutomatchCancelled -> onAutomatchCancelled(action.automatch)
            is Action.LoadMoreHistoricGames -> onNeedMoreOlderGames(action.game)
            is Action.DismissWhatsNewDialog -> onDismissWhatsNewDialog()
            Action.DismissAlertDialog -> onDismissAlertDialog()
            Action.GameNavigationConsumed -> onGameNavigationConsumed()
            Action.ViewResumed -> onViewResumed()

            Action.CustomGame, is Action.GameSelected, Action.PlayAgainstAI, Action.PlayOnline, Action.SupportClicked -> {} // intentionally left blank
        }
    }

    private fun onViewResumed() {
        chatRepository.fetchRecentChatMessages()
        _state.value = _state.value.copy(
            // Check if board theme had been changed in the settings
            boardTheme = settingsRepository.boardTheme
        )
    }

    private fun onGameNavigationConsumed() {
        _state.value = _state.value.copy(
            gameNavigationPending = null
        )
    }

    private fun onDismissAlertDialog() {
        _state.value = _state.value.copy(
            alertDialogText = null,
            alertDialogTitle = null
        )
    }

    private fun onDismissWhatsNewDialog() {
        WhatsNewUtils.textShown()
        _state.value = _state.value.copy(
            whatsNewDialogVisible = false
        )
    }

    private fun onGameStart(game: Game) {
        _state.value = _state.value.copy(
            gameNavigationPending = game
        )
    }

    private fun onNeedMoreOlderGames(lastGame: Game?) {
        loadOlderGamesSubscription?.dispose()
        loadOlderGamesSubscription =
            finishedGamesRepository.getHistoricGames(lastGame?.ended)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .distinctUntilChanged()
                .doOnNext {
                    _state.value = _state.value.copy(
                        loadingHistoricGames = it.loading,
                        loadedAllHistoricGames = it.loadedLastPage
                    )
                }
                .map { it.games }
                .map(this::computePositions)
                .subscribe(this::onHistoricGames, this::onError)
        loadOlderGamesSubscription?.addToDisposable(subscriptions)
    }

    private fun computePositions(games: List<Game>): List<Game> =
        games.onEach { it.position = RulesManager.replay(it, computeTerritory = false) }

    private fun onHistoricGames(games: List<Game>) {
        val existingGames = _state.value.historicGames
        val newGames = games.filter { candidate -> existingGames.find { candidate.id == it.id } == null }
        _state.value = _state.value.copy(
            historicGames = existingGames + newGames
        )
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
    val userId: Long,
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
    val boardTheme: BoardTheme,
)


sealed class Action {
    object PlayOnline: Action()
    object CustomGame: Action()
    object PlayAgainstAI: Action()
    object SupportClicked: Action()
    object DismissWhatsNewDialog: Action()
    object DismissAlertDialog: Action()
    object GameNavigationConsumed: Action()
    class GameSelected(val game: Game): Action()
    class ChallengeCancelled(val challenge: Challenge): Action()
    class ChallengeAccepted(val challenge: Challenge): Action()
    class ChallengeDeclined(val challenge: Challenge): Action()
    class AutomatchCancelled(val automatch: OGSAutomatch): Action()
    class LoadMoreHistoricGames(val game: Game?): Action()
    object ViewResumed: Action()
}
