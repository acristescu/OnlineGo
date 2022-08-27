package io.zenandroid.onlinego.usecases

import com.github.mikephil.charting.data.Entry
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import io.zenandroid.onlinego.data.ogs.OGSRestService
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.util.*

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

    private fun generateChartData(duration: Long?, groupCount: Int, rawData: List<Glicko2HistoryItem>): List<Entry> {
        if(rawData.isEmpty()) {
            return emptyList()
        }
        val targetDate = duration?.let { LocalDateTime.now().minusSeconds(duration).toEpochSecond(ZoneOffset.UTC) } ?: rawData.first().ended
        val groupWidth = ( LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - targetDate ) / groupCount.toFloat()
        var currentRank = 0f
        var dataIndex = 0
        return (0 until groupCount).map { i ->
            val x = targetDate + i * groupWidth
            while(dataIndex < rawData.size && rawData[dataIndex].ended < x) {
                currentRank = rawData[dataIndex].rating
                dataIndex++
            }
            Entry(x, currentRank)
        }.filter { it.y != 0f }
    }

    private val month = 60 * 60 * 24 * 30L
    private val year = 60 * 60 * 24 * 356L

    private fun processPlayerStats(history: Glicko2History): UserStats {
        var highestRating: Float? = null
        var highestRatingTimestamp: Long? = null
        history.history.maxByOrNull { it.rating }?.let {
            highestRating = it.rating
            highestRatingTimestamp = it.ended
        }
        val rawData = history.history.sortedBy { it.ended }
        val chartAll = generateChartData(null, 75, rawData)
        val chart1M = generateChartData(month, 30, rawData)
        val chart3M = generateChartData(3 * month, 60, rawData)
        val chart1Y = generateChartData(year, 75, rawData)
        val chart5Y = generateChartData(5 * year, 75, rawData)
        
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
            chartData1M = chart1M,
            chartData3M = chart3M,
            chartData1Y = chart1Y,
            chartData5Y = chart5Y,
            chartDataAll = chartAll,
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
