package io.zenandroid.onlinego.data.model.local

import com.github.mikephil.charting.data.Entry
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import javax.annotation.concurrent.Immutable

@Immutable
data class UserStats(
    val highestRating: Float?,
    val highestRatingTimestamp: Long?,
    val chartDataAll: List<Entry>,
    val chartData1M: List<Entry>,
    val chartData3M: List<Entry>,
    val chartData1Y: List<Entry>,
    val chartData5Y: List<Entry>,
    val wonCount: Int,
    val lostCount: Int,
    val bestStreak: Int,
    val bestStreakStart: Long,
    val bestStreakEnd: Long,
    val mostFacedId: Long?,
    val mostFacedGameCount: Int,
    val mostFacedWon: Int,
    val highestWin: Glicko2HistoryItem?,
    val last10Games: List<Glicko2HistoryItem>
) {
    companion object {
        val EMPTY = UserStats(
            0f, 0L, emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), 0, 0, 0, 0L, 0L, null, 0, 0, null, emptyList()
        )
    }
}
