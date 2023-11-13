package io.zenandroid.onlinego.usecases

import com.github.mikephil.charting.data.Entry
import io.zenandroid.onlinego.data.model.local.HistoryItem
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.local.WinLossStats
import io.zenandroid.onlinego.data.model.local.toHistoryItem
import io.zenandroid.onlinego.data.model.ogs.VersusStats
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.usecases.RepoResult.Error
import io.zenandroid.onlinego.usecases.RepoResult.Success
import io.zenandroid.onlinego.utils.recordException
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.min

class GetUserStatsUseCase (
    private val restService: OGSRestService,
){

    suspend fun getPlayerStatsAsync(playerId: Long): RepoResult<UserStats> {
        return try {
            val history = restService.getPlayerStatsAsync(playerId).history.map { it.toHistoryItem("overall", 0) }
            Success(processPlayerStats(history))
        } catch (e: Exception) {
            recordException(e)
            Error(e)
        }
    }

    suspend fun getPlayerStatsWithSizesAsync(playerId: Long): Result<UserStats> {
        return try {
            val sizes = listOf(9, 13, 19)
            val speeds = listOf("blitz", "live", "correspondence")
            val allHistory: MutableList<HistoryItem> = mutableListOf()
            sizes.forEach { size ->
                speeds.forEach { speed ->
                    allHistory.addAll(
                        restService.getPlayerStatsAsync(playerId, speed, size )
                            .history
                            .map { it.toHistoryItem(speed, size) }
                    )
                }
            }
            val historyWithOverallELO = restService.getPlayerStatsAsync(playerId).history.map { it.toHistoryItem("overall", 0) }
            val processedList = allHistory.sortedBy { -it.ended }
                .map {
                    val overallElo = historyWithOverallELO.find { overall ->
                        overall.gameId == it.gameId
                    }?.rating
                    it.copy(
                        rating = overallElo ?: it.rating
                    )
                }

            Result.success(processPlayerStats(processedList))
        } catch (e: Exception) {
            recordException(e)
            Result.failure(e)
        }
    }

    suspend fun getVSStats(playerId: Long): RepoResult<VersusStats> {
        return try {
            Success(restService.getPlayerVersusStats(playerId))
        } catch (e: Exception) {
            recordException(e)
            Error(e)
        }
    }

    private fun generateChartDataByDuration(duration: Long?, groupCount: Int, rawData: List<HistoryItem>): List<Entry> {
        if(rawData.isEmpty()) {
            return emptyList()
        }
        val targetDate = duration?.let { LocalDateTime.now().minusSeconds(duration).toEpochSecond(
            ZoneOffset.UTC) } ?: rawData.first().ended
        val groupWidth = ( LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - targetDate ) / groupCount.toFloat()
        var currentRank = 0f
        var dataIndex = 0
        return (0 until groupCount).map { i ->
            val x = targetDate + i * groupWidth
            while(dataIndex < rawData.size && rawData[dataIndex].ended <= x) {
                currentRank = rawData[dataIndex].rating
                dataIndex++
            }
            Entry(x, currentRank)
        }.filter { it.y != 0f }
    }

    private fun generateChartDataByGame(gameCount: Int?, rawData: List<HistoryItem>): List<Entry> {
        if(rawData.isEmpty()) {
            return emptyList()
        }
        val gameCount = gameCount?.coerceAtMost(rawData.size) ?: rawData.size
        val dataIndex = rawData.size - gameCount
        return (dataIndex until rawData.size).map { x ->
            Entry(x.toFloat(), rawData[x].rating)
        }.filter { it.y != 0f }
    }

    private val month = 60 * 60 * 24 * 30L
    private val year = 60 * 60 * 24 * 356L

    private fun processPlayerStats(history: List<HistoryItem>): UserStats {
        var highestRating: Float? = null
        var highestRatingTimestamp: Long? = null
        history.maxByOrNull { it.rating }?.let {
            highestRating = it.rating
            highestRatingTimestamp = it.ended
        }
        val rawData = history.sortedBy { it.ended }
        val chartAll = generateChartDataByDuration(null, 75, rawData)
        val chart1M = generateChartDataByDuration(month, 30, rawData)
        val chart3M = generateChartDataByDuration(3 * month, 60, rawData)
        val chart1Y = generateChartDataByDuration(year, 75, rawData)
        val chart5Y = generateChartDataByDuration(5 * year, 75, rawData)
        val chart20G = generateChartDataByGame(20, rawData)
        val chart100G = generateChartDataByGame(100, rawData)
        val chartAllG = generateChartDataByGame(null, rawData)
        
        val wonCount = history.count { it.won }
        val lostCount = history.size - wonCount

        var streakCount = 0
        var streakStart = 0L
        var streakEnd = 0L
        var bestStreak = 0
        var bestStreakStart = 0L
        var bestStreakEnd = 0L
        var smallBoard = 0
        var mediumBoard = 0
        var largeBoard = 0
        var smallBoardWon = 0
        var mediumBoardWon = 0
        var largeBoardWon = 0
        var blitz = 0
        var blitzWon = 0
        var live = 0
        var liveWon = 0
        var correspondence = 0
        var correspondenceWon = 0
        var white = 0
        var whiteWon = 0
        var black = 0
        var blackWon = 0

        for (game in history) {
            when(game.size) {
                9 -> {
                    smallBoard++
                    if(game.won) {
                        smallBoardWon++
                    }
                }
                13 -> {
                    mediumBoard++
                    if(game.won) {
                        mediumBoardWon++
                    }
                }
                19 -> {
                    largeBoard++
                    if(game.won) {
                        largeBoardWon++
                    }
                }
            }
            when(game.speed) {
                "blitz" -> {
                    blitz++
                    if(game.won) {
                        blitzWon++
                    }
                }
                "live" -> {
                    live++
                    if(game.won) {
                        liveWon++
                    }
                }
                "correspondence" -> {
                    correspondence++
                    if(game.won) {
                        correspondenceWon++
                    }
                }
            }
            if(game.playedBlack) {
                black++
                if(game.won) {
                    blackWon++
                }
            } else {
                white++
                if(game.won) {
                    whiteWon++
                }
            }
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

        val mostFacedId = history
            .groupingBy { it.opponentId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val mostFacedGameList = history.filter { it.opponentId == mostFacedId }

        val highestWin = history
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
            chartData20G = chart20G,
            chartData100G = chart100G,
            chartDataAllG = chartAllG,
            wonCount = wonCount,
            lostCount = lostCount,
            bestStreak = bestStreak,
            bestStreakStart = bestStreakStart,
            bestStreakEnd = bestStreakEnd,
            mostFacedId = mostFacedId,
            mostFacedGameCount = mostFacedGameList.size,
            mostFacedWon = mostFacedGameList.count { it.won },
            highestWin = highestWin,
            last10Games = history.take(10).reversed(),
            allGames = statsOf(history.size, history.size, wonCount),
            smallBoard = statsOf(history.size, smallBoard, smallBoardWon),
            mediumBoard = statsOf(history.size, mediumBoard, mediumBoardWon),
            largeBoard = statsOf(history.size, largeBoard, largeBoardWon),
            blitz = statsOf(history.size, blitz, blitzWon),
            live = statsOf(history.size, live, liveWon),
            asWhite = statsOf(history.size, white, whiteWon),
            asBlack = statsOf(history.size, black, blackWon),
            correspondence = statsOf(history.size, correspondence, correspondenceWon),
        )
    }

    private fun statsOf(total: Int, matching: Int, winning: Int) =
        WinLossStats(
            total = matching,
            totalRatio = if (total == 0) 0f else matching.toFloat() / total,
            won = winning,
            lost = matching - winning,
            winRate = if(matching == 0) 0f else winning.toFloat() / matching,
            lossRate = if(matching == 0) 0f else 1f - (winning.toFloat() / matching)
        )
}

sealed interface RepoResult<T> {
    class Loading<T>: RepoResult<T>
    class Error<T>(val throwable: Throwable): RepoResult<T>
    class Success<T>(val data: T): RepoResult<T>
}
