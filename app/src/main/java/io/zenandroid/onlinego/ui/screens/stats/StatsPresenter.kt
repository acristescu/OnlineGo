package io.zenandroid.onlinego.ui.screens.stats

import android.util.Log
import com.github.mikephil.charting.data.Entry
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.addToDisposable

/**
 * Created by alex on 05/11/2017.
 */
class StatsPresenter(
        private val view: StatsContract.View,
        private val analytics: FirebaseAnalytics,
        private val restService: OGSRestService,
        private val playerId: Long
) : StatsContract.Presenter {

    private val subscriptions = CompositeDisposable()
    private var highestWin: Glicko2HistoryItem? = null

    override fun subscribe() {
        restService.getPlayerProfile(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::fillPlayerDetails, this::onError)
                .addToDisposable(subscriptions)

        restService.getPlayerStats(playerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::fillPlayerStats, this::onError)
                .addToDisposable(subscriptions)
    }

    private fun fillPlayerDetails(playerDetails: OGSPlayer) {
        view.fillPlayerDetails(playerDetails)
    }

    private fun fillPlayerStats(history: Glicko2History) {

        history.history.maxBy { it.rating }?.let {
            view.fillHighestRank(it.rating, it.ended)
        }

        var lastAddedTimestamp = 0L
        val entries = history.history
                .filterIndexed { index, item ->
                    val take = (index == 0) || lastAddedTimestamp > item.ended + 60*60*24
                    if(take) {
                        lastAddedTimestamp = item.ended
                    }
                    take
                }
                .map { Entry(it.ended / 60 / 60f, it.rating) }
        view.fillRankGraph(entries)

        val wonCount = history.history.count { it.won }
        val lostCount = history.history.size - wonCount
        view.fillOutcomePieChart(lostCount, wonCount)

        view.fillCurrentForm(history.history.take(10).reversed())

        var streakCount = 0
        var streakStart = 0L
        var streakEnd = 0L
        var bestStreak = 0
        var bestStreakStart = 0L
        var bestStreakEnd = 0L

        for (game in history.history) {
            if (game.won) {
                if (streakCount == 0) {
                    streakEnd = game.ended
                }
                streakStart = game.ended
                streakCount++
            } else {
                if (streakCount > bestStreak) {
                    bestStreak = streakCount
                    bestStreakStart = streakStart
                    bestStreakEnd = streakEnd
                }
                streakCount = 0
            }
        }
        view.fillLongestStreak(bestStreak, bestStreakStart, bestStreakEnd)

        val mostFacedId = history.history
                .groupingBy { it.opponentId }
                .eachCount()
                .maxBy { it.value }
                ?.key

        if(mostFacedId != null) {
            val gameList = history.history.filter { it.opponentId == mostFacedId }
            val won = gameList.count { it.won }
            restService.getPlayerProfile(mostFacedId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        view.mostFacedOpponent(it, gameList.size, won)
                    }, this::onError)
                    .addToDisposable(subscriptions)
        } else {
            //TODO
        }

        highestWin = history.history
                .filter { it.won }
                .filter { it.opponentDeviation < 100 } // excludes provisional players
                .maxBy { it.opponentRating }

        highestWin?.let { winningGame ->
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
        FirebaseCrashlytics.getInstance().recordException(t)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}