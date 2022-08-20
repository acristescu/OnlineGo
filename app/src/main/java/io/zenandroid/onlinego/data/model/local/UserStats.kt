package io.zenandroid.onlinego.data.model.local

import com.github.mikephil.charting.data.Entry
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import javax.annotation.concurrent.Immutable

@Immutable
data class UserStats(
    val highestRating: Float?,
    val highestRatingTimestamp: Long?,
    val rankData: List<Entry>,
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
)
