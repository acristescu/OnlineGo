package io.zenandroid.onlinego.ui.screens.stats

import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.addToDisposable

/**
 * Created by alex on 05/11/2017.
 */
class StatsPresenter(
        val view: StatsContract.View,
        val analytics: FirebaseAnalytics,
        val restService: OGSRestService,
        val playerId: Long) : StatsContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        restService.getPlayerProfile(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({fillPlayerDetails(it)}, {})
                .addToDisposable(subscriptions)

        restService.getPlayerStats(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({fillPlayerStats(it)}, {})
                .addToDisposable(subscriptions)
    }

    private fun fillPlayerDetails(playerDetails: OGSPlayer) {
        view.fillPlayerDetails(playerDetails)
    }

    private fun fillPlayerStats(playerStats: String) {
        var opponentInfo = view.fillPlayerStats(playerStats)

        restService.getPlayerProfile(opponentInfo.first)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({mostFacedOpponent(it)}, {})
                .addToDisposable(subscriptions)

        restService.getPlayerProfile(opponentInfo.second)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({highestWin(it)}, {})
                .addToDisposable(subscriptions)
    }

    private fun mostFacedOpponent(playerDetails: OGSPlayer) {
        view.mostFacedOpponent(playerDetails)
    }

    private fun highestWin(playerDetails: OGSPlayer) {
        view.highestWin(playerDetails)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}