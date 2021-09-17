package io.zenandroid.onlinego.ui.screens.mygames

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.addToDisposable
import javax.annotation.concurrent.Immutable

class MyGamesViewModel(
    private val userSessionRepository: UserSessionRepository,
    private val analytics: FirebaseAnalytics,
    private val restService: OGSRestService,
    private val socketService: OGSWebSocketService,
    ) : ViewModel() {
    private val _state = MutableLiveData(MyGamesState(userId = userSessionRepository.userId ?: 0))
    val state: LiveData<MyGamesState> = _state
    private val subscriptions = CompositeDisposable()

    override fun onCleared() {
        subscriptions.clear()
        super.onCleared()
    }

    fun setGames(games: List<Game>) {
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

        _state.value = _state.value?.copy(
            myTurnGames = myTurnList,
            opponentTurnGames = opponentTurnList
        )
    }

    fun setRecentGames(games: List<Game>) {
        _state.value = _state.value?.copy(
            recentGames = games
        )
    }

    fun setChallenges(challenges: List<Challenge>) {
        _state.value = _state.value?.copy(
            challenges = challenges
        )
    }


    fun setAutomatches(automatches: List<OGSAutomatch>) {
        _state.value = _state.value?.copy(
            automatches = automatches
        )
    }


    fun onChallengeCancelled(challenge: Challenge) {
        analytics.logEvent("challenge_cancelled", null)
        restService.declineChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    fun onChallengeAccepted(challenge: Challenge) {
        analytics.logEvent("challenge_accepted", null)
        restService.acceptChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    fun onChallengeDeclined(challenge: Challenge) {
        analytics.logEvent("challenge_declined", null)
        restService.declineChallenge(challenge.id)
            .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
            .subscribe({}, this::onError)
            .addToDisposable(subscriptions)
    }

    fun onAutomatchCancelled(automatch: OGSAutomatch) {
        analytics.logEvent("new_game_cancelled", null)
        socketService.cancelAutomatch(automatch)
    }


    private fun onError(t: Throwable) {
        if(t is retrofit2.HttpException) {
            if(t.code() in arrayOf(401, 403)) {
                FirebaseCrashlytics.getInstance().setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
                userSessionRepository.logOut()
                _state.value = _state.value?.copy(
                    userIsLoggedOut = true
                )
            } else {
                FirebaseCrashlytics.getInstance().recordException(Exception(t.response()?.errorBody()?.string(), t))
            }
        } else {
            if(t is com.squareup.moshi.JsonDataException) {
                _state.value = _state.value?.copy(
                    errorMessage = "An error occurred white talking to the OGS Server. This usually means the website devs have changed something in the API. Please report this error as the app will probably not work until we adapt to this change."
                )
            }
            FirebaseCrashlytics.getInstance().recordException(t)
        }

        Log.e(MyGamesPresenter.TAG, t.message, t)
    }

    fun onAction(action: Action) {
        when(action) {
            is Action.ChallengeAccepted -> onChallengeAccepted(action.challenge)
            is Action.ChallengeCancelled -> onChallengeCancelled(action.challenge)
            is Action.ChallengeDeclined -> onChallengeDeclined(action.challenge)
            is Action.AutomatchCancelled -> onAutomatchCancelled(action.automatch)

            Action.CustomGame, is Action.GameSelected, Action.PlayOffline, Action.PlayOnline -> {} // intentionally left blank
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
    val userId: Long,
    val userIsLoggedOut: Boolean = false,
    val errorMessage: String? = null
)