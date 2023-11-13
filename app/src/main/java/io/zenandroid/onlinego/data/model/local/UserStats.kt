package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable
import com.github.mikephil.charting.data.Entry

@Immutable
data class UserStats(
    val highestRating: Float?,
    val highestRatingTimestamp: Long?,
    val chartDataAll: List<Entry>,
    val chartData1M: List<Entry>,
    val chartData3M: List<Entry>,
    val chartData1Y: List<Entry>,
    val chartData5Y: List<Entry>,
    val chartData20G: List<Entry>,
    val chartData100G: List<Entry>,
    val chartDataAllG: List<Entry>,
    val wonCount: Int,
    val lostCount: Int,
    val bestStreak: Int,
    val bestStreakStart: Long,
    val bestStreakEnd: Long,
    val mostFacedId: Long?,
    val mostFacedGameCount: Int,
    val mostFacedWon: Int,
    val highestWin: HistoryItem?,
    val last10Games: List<HistoryItem>,
    val allGames: WinLossStats,
    val smallBoard: WinLossStats,
    val mediumBoard: WinLossStats,
    val largeBoard: WinLossStats,
    val blitz: WinLossStats,
    val live: WinLossStats,
    val asBlack: WinLossStats,
    val asWhite: WinLossStats,
    val correspondence: WinLossStats,
) {
    companion object {
        val EMPTY = UserStats(
            0f, 0L,
            listOf(), listOf(), listOf(), listOf(), listOf(),
            listOf(), listOf(), listOf(),
            0, 0, 0, 0L, 0L, null, 0, 0, null, emptyList(),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
            WinLossStats(0, 0f, 0, 0, 0f, 0f),
        )
    }
}

@Immutable
data class WinLossStats(
    val total: Int,
    val totalRatio: Float,
    val won: Int,
    val lost: Int,
    val winRate: Float,
    val lossRate: Float,
)
