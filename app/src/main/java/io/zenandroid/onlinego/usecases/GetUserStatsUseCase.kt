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

    suspend fun getPlayerStatsWithSizesAsync(playerId: Long): RepoResult<UserStats> {
        return try {
            val smallHistory = restService.getPlayerStatsAsync(playerId, "overall", 9 ).history.map { it.toHistoryItem("overall", 9) }
            val mediumHistory = restService.getPlayerStatsAsync(playerId, "overall", 13).history.map { it.toHistoryItem("overall", 13) }
            val largeHistory = restService.getPlayerStatsAsync(playerId, "overall", 19).history.map { it.toHistoryItem("overall", 19) }
            val history = restService.getPlayerStatsAsync(playerId).history.map { it.toHistoryItem("overall", 0) }

            val allHistory = listOf(smallHistory, mediumHistory, largeHistory)
                .flatten()
                .sortedBy { -it.ended }

            val missing = history.filterNot { x ->  allHistory.any { it.gameId == x.gameId } }
            missing.forEach { println("*** $it") }

            Success(processPlayerStats(allHistory))
        } catch (e: Exception) {
            recordException(e)
            Error(e)
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

    private fun generateChartData(duration: Long?, groupCount: Int, rawData: List<HistoryItem>): List<Entry> {
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
        val chartAll = generateChartData(null, 75, rawData)
        val chart1M = generateChartData(month, 30, rawData)
        val chart3M = generateChartData(3 * month, 60, rawData)
        val chart1Y = generateChartData(year, 75, rawData)
        val chart5Y = generateChartData(5 * year, 75, rawData)
        
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
            allGames = WinLossStats(history.size, 1f, wonCount, lostCount, wonCount.toFloat() / history.size, lostCount.toFloat() / history.size),
            smallBoard = WinLossStats(
                total = smallBoard,
                totalRatio = if (history.isEmpty()) 0f else smallBoard.toFloat() / history.size,
                won = smallBoardWon,
                lost = smallBoard - smallBoardWon,
                winRate = smallBoardWon.toFloat() / smallBoard,
                lossRate = 1f - (smallBoardWon.toFloat() / smallBoard)
            ),
            mediumBoard = WinLossStats(
                total = mediumBoard,
                totalRatio = if (history.isEmpty()) 0f else mediumBoard.toFloat() / history.size,
                won = mediumBoardWon,
                lost = mediumBoard - mediumBoardWon,
                winRate = mediumBoardWon.toFloat() / mediumBoard,
                lossRate = 1f - (mediumBoardWon.toFloat() / mediumBoard)
            ),
            largeBoard = WinLossStats(
                total = largeBoard,
                totalRatio = if (history.isEmpty()) 0f else largeBoard.toFloat() / history.size,
                won = largeBoardWon,
                lost = largeBoard - largeBoardWon,
                winRate = largeBoardWon.toFloat() / largeBoard,
                lossRate = 1f - (largeBoardWon.toFloat() / largeBoard)
            ),
        )
    }
}

sealed interface RepoResult<T> {
    class Loading<T>: RepoResult<T>
    class Error<T>(val throwable: Throwable): RepoResult<T>
    class Success<T>(val data: T): RepoResult<T>
}