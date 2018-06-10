package io.zenandroid.onlinego.main

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.OGSServiceImpl

/**
 * Created by alex on 14/03/2018.
 */
class MainPresenter (val view : MainContract.View, val activeGameRepository: ActiveGameRepository) : MainContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var lastGameNotified: DbGame? = null

    override fun subscribe() {
        view.mainTitle = "OnlineGo"
        view.subtitle = BuildConfig.VERSION_NAME


        subscriptions.add(
                OGSServiceImpl.instance.loginWithToken().
                        subscribe {
                            OGSServiceImpl.instance.ensureSocketConnected()
                            OGSServiceImpl.instance.resendAuth()
                        }
        )
        subscriptions.add(
                activeGameRepository.myMoveCountObservable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onMyMoveCountChanged)
        )
        activeGameRepository.subscribe()
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
        }
    }

    override fun unsubscribe() {
        activeGameRepository.unsubscribe()
        subscriptions.clear()
        OGSServiceImpl.instance.disconnect()
    }

    fun navigateToGameScreenById(gameId: Long) {
        subscriptions.add(
            activeGameRepository.getGameSingle(gameId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(view::navigateToGameScreen, this::onError)
        )
    }

    override fun onNotificationClicked() {
        lastGameNotified = if(lastGameNotified == null) {
            activeGameRepository.myTurnGamesList[0]
        } else {
            val index = activeGameRepository.myTurnGamesList.indexOf(lastGameNotified!!)
            if(index == -1) {
                activeGameRepository.myTurnGamesList[0]
            } else {
                activeGameRepository.myTurnGamesList[(index + 1) % activeGameRepository.myTurnGamesList.size]
            }
        }
        view.navigateToGameScreen(lastGameNotified!!)
    }

    private fun onError(t: Throwable) {
        Log.e(MainActivity.TAG, t.message, t)
        view.showError(t.message)
    }
}