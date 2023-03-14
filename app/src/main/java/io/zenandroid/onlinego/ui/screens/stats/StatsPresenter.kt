package io.zenandroid.onlinego.ui.screens.stats

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.ui.screens.stats.StatsContract.Filter.ALL
import io.zenandroid.onlinego.ui.screens.stats.StatsContract.Filter.FIVE_YEARS
import io.zenandroid.onlinego.ui.screens.stats.StatsContract.Filter.ONE_MONTH
import io.zenandroid.onlinego.ui.screens.stats.StatsContract.Filter.ONE_YEAR
import io.zenandroid.onlinego.ui.screens.stats.StatsContract.Filter.THREE_MONTHS
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException

/**
 * Created by alex on 05/11/2017.
 */
class StatsPresenter(
        private val view: StatsContract.View,
        private val analytics: FirebaseAnalytics,
        private val restService: OGSRestService,
        private val getUserStatsUseCase: GetUserStatsUseCase,
        private val playerId: Long
) : StatsContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var stats: UserStats? = null
    override var currentFilter = ONE_MONTH
    set(value) {
        field = value
        stats?.let {
            view.fillRankGraph(when(value) {
                ONE_MONTH -> it.chartData1M
                THREE_MONTHS -> it.chartData3M
                ONE_YEAR -> it.chartData1Y
                FIVE_YEARS -> it.chartData5Y
                ALL -> it.chartDataAll
            })
        }
    }

    override fun subscribe() {
        restService.getPlayerProfile(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::fillPlayerDetails, this::onError)
                .addToDisposable(subscriptions)

        getUserStatsUseCase.getPlayerStats(playerId)
                .subscribe(this::fillPlayerStats, this::onError)
                .addToDisposable(subscriptions)
    }

    private fun fillPlayerDetails(playerDetails: OGSPlayer) {
        view.fillPlayerDetails(playerDetails)
    }

    private fun fillPlayerStats(stats: UserStats) {
        this.stats = stats

        if(stats.highestRating != null && stats.highestRatingTimestamp != null) {
            view.fillHighestRank(stats.highestRating, stats.highestRatingTimestamp)
        }
        view.fillRankGraph(when(currentFilter) {
            ONE_MONTH -> stats.chartData1M
            THREE_MONTHS -> stats.chartData3M
            ONE_YEAR -> stats.chartData1Y
            FIVE_YEARS -> stats.chartData5Y
            ALL -> stats.chartDataAll
        })
        view.fillOutcomePieChart(stats.lostCount, stats.wonCount)
        view.fillCurrentForm(stats.last10Games)
        view.fillLongestStreak(stats.bestStreak, stats.bestStreakStart, stats.bestStreakEnd)

        if(stats.mostFacedId != null) {
            restService.getPlayerProfile(stats.mostFacedId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    view.mostFacedOpponent(it, stats.mostFacedGameCount, stats.mostFacedWon)
                }, this::onError)
                .addToDisposable(subscriptions)
        }

        stats.highestWin?.let { winningGame ->
            restService.getPlayerProfile(winningGame.opponentId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    view.fillHighestWin(it, winningGame)
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

    override fun unsubscribe() {
        subscriptions.clear()
    }
}