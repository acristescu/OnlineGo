package io.zenandroid.onlinego.ui.screens.main

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.ui.screens.newchallenge.ChallengeParams
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AutomatchRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 14/03/2018.
 */
class MainPresenter (
        private val view : MainContract.View,
        private val restService: OGSRestService,
        private val socketService: OGSWebSocketService,
        private val userSessionRepository: UserSessionRepository,
        private val automatchRepository: AutomatchRepository,
        private val activeGameRepository: ActiveGamesRepository
) : MainContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var lastGameNotified: Game? = null
    private var lastMoveCount: Int? = null

    override fun subscribe() {
        if(userSessionRepository.isLoggedIn()) {
            socketService.ensureSocketConnected()
            socketService.resendAuth()
        } else {
            view.showLogin()
        }

        activeGameRepository.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMyMoveCountChanged)
                .addToDisposable(subscriptions)
        Observable.interval(10, TimeUnit.SECONDS).subscribe {
            socketService.ensureSocketConnected()
        }.addToDisposable(subscriptions)

//        activeGameRepository.subscribe()
    }

    private fun onMyMoveCountChanged(myMoveCount: Int) {
        if (myMoveCount == 0) {
            view.notificationsButtonEnabled = false
            view.notificationsBadgeVisible = false
            view.cancelNotification()
        } else {
//            val sortedMyTurnGames = activeGameRepository.myTurnGamesList.sortedWith(compareBy { it.id })
            view.notificationsButtonEnabled = true
            view.notificationsBadgeVisible = true
            view.notificationsBadgeCount = myMoveCount.toString()
//            view.updateNotification(sortedMyTurnGames)
            lastMoveCount?.let {
                if(myMoveCount > it) {
                    view.vibrate()
                }
            }
        }
        lastMoveCount = myMoveCount
    }

    override fun unsubscribe() {
        subscriptions.clear()
        socketService.disconnect()
    }

    fun navigateToGameScreenById(gameId: Long) {
        activeGameRepository.getGameSingle(gameId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::navigateToGameScreen, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onNotificationClicked() {
        val gamesList = activeGameRepository.myTurnGamesList
        if(gamesList.isEmpty()) {
            FirebaseCrashlytics.getInstance().log("Notification clicked while no games available")
            return
        }
        val gameToNavigate = if(lastGameNotified == null) {
            gamesList[0]
        } else {
            val index = gamesList.indexOfFirst { it.id == lastGameNotified?.id }
            if(index == -1) {
                gamesList[0]
            } else {
                gamesList[(index + 1) % gamesList.size]
            }
        }
        lastGameNotified = gameToNavigate
        view.navigateToGameScreen(gameToNavigate)
    }

    override fun onStartSearch(sizes: List<Size>, speed: Speed) {
        if(speed in arrayOf(Speed.NORMAL, Speed.BLITZ) && automatchRepository.automatches.find { it.liveOrBlitz } != null) {
            view.showError("Can only search for one live or blitz game at a time.")
        } else {
            socketService.startAutomatch(sizes, speed)
        }
    }

    override fun onNewBotChallenge(challengeParams: ChallengeParams) {
        restService.challengeBot(challengeParams)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onNewFriendChallenge(challengeParams: ChallengeParams) {
        restService.challengeBot(challengeParams)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    private fun onError(t: Throwable) {
        Log.e(MainActivity.TAG, t.message, t)
        view.showError(t.message)
    }
}