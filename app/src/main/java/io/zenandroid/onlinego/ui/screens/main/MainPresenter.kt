package io.zenandroid.onlinego.ui.screens.main

import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
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
        private val automatchRepository: AutomatchRepository
) : MainContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var hasAskedForPermissionsAlready = false

    override fun subscribe() {
        if(userSessionRepository.isLoggedIn()) {
            if(!hasAskedForPermissionsAlready) {
                hasAskedForPermissionsAlready = true
                view.askForNotificationsPermission(false)
            }
            socketService.ensureSocketConnected()
            socketService.resendAuth()
        } else {
            view.showLogin()
        }

        Observable.interval(10, TimeUnit.SECONDS).subscribe {
            if(userSessionRepository.isLoggedIn()) {
                socketService.ensureSocketConnected()
            }
        }.addToDisposable(subscriptions)

    }

    override fun unsubscribe() {
        subscriptions.clear()
        socketService.disconnect()
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