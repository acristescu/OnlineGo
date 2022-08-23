package io.zenandroid.onlinego.usecases

import com.github.mikephil.charting.data.Entry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.ogs.OGSRestService

class GetUserStatsUseCase (
    private val restService: OGSRestService,
){

    fun getPlayerStats(playerId: Long) =
        restService.getPlayerStats(playerId)
            .map (this::processPlayerStats)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    suspend fun getPlayerStatsAsync(playerId: Long): UserStats {
        val history = restService.getPlayerStatsAsync(playerId)
        return processPlayerStats(history)
    }

    private fun processPlayerStats(history: Glicko2History): UserStats {
        var highestRating: Float? = null
        var highestRatingTimestamp: Long? = null
        history.history.maxByOrNull { it.rating }?.let {
            highestRating = it.rating
            highestRatingTimestamp = it.ended
        }

        val groupCount = 150
        val newest = history.history.firstOrNull()?.ended ?: 0
        val oldest = history.history.firstOrNull()?.ended ?: 0
        val groupWidth = ( oldest - newest ) / groupCount.toFloat()
        val groups = history.history.groupBy { ((it.ended - oldest) / groupWidth).toInt() }
            .map { (_, group) ->
                val avgDate = group.sumByDouble { it.ended.toDouble() } / group.size
                val avgRating = group.sumByDouble { it.rating.toDouble() } / group.size
                Entry(avgDate.toFloat(), avgRating.toFloat())
            }

        val wonCount = history.history.count { it.won }
        val lostCount = history.history.size - wonCount

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

        val mostFacedId = history.history
            .groupingBy { it.opponentId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val mostFacedGameList = history.history.filter { it.opponentId == mostFacedId }

        val highestWin = history.history
            .filter { it.won }
            .filter { it.opponentDeviation < 100 } // excludes provisional players
            .maxByOrNull { it.opponentRating }


        return UserStats(
            highestRating = highestRating,
            highestRatingTimestamp = highestRatingTimestamp,
            rankData = groups,
            wonCount = wonCount,
            lostCount = lostCount,
            bestStreak = bestStreak,
            bestStreakStart = bestStreakStart,
            bestStreakEnd = bestStreakEnd,
            mostFacedId = mostFacedId,
            mostFacedGameCount = mostFacedGameList.size,
            mostFacedWon = mostFacedGameList.count { it.won },
            highestWin = highestWin,
            last10Games = history.history.take(10).reversed(),
        )
    }
}
