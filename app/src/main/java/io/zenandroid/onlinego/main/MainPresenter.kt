package io.zenandroid.onlinego.main

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.Size
import io.zenandroid.onlinego.model.ogs.Speed
import io.zenandroid.onlinego.newchallenge.ChallengeParams
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.AutomatchRepository
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 14/03/2018.
 */
class MainPresenter (
        private val view : MainContract.View,
        private val activeGameRepository: ActiveGameRepository
) : MainContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var lastGameNotified: Game? = null
    private var lastMoveCount: Int? = null

    override fun subscribe() {
        if(OGSServiceImpl.instance.isLoggedIn()) {
            OGSServiceImpl.instance.ensureSocketConnected()
            OGSServiceImpl.instance.resendAuth()
        } else {
            view.showLogin()
        }

        activeGameRepository.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMyMoveCountChanged)
                .addToDisposable(subscriptions)
        Observable.interval(10, TimeUnit.SECONDS).subscribe {
            OGSServiceImpl.instance.ensureSocketConnected()
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
//        activeGameRepository.unsubscribe()
        subscriptions.clear()
        OGSServiceImpl.instance.disconnect()
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
            Crashlytics.log("Notification clicked while no games available")
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
        if(speed in arrayOf(Speed.NORMAL, Speed.BLITZ) && AutomatchRepository.automatches.find { it.liveOrBlitz } != null) {
            view.showError("Can only search for one live or blitz game at a time.")
        } else {
            OGSServiceImpl.instance.startAutomatch(sizes, speed)
        }
    }

    override fun onNewBotChallenge(challengeParams: ChallengeParams) {
        OGSServiceImpl.instance.challengeBot(challengeParams)
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