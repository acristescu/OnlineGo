package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.local.Player
import java.time.Instant

@Immutable
data class OGSPlayerProfile (
    val user: Player,
    val vs: VersusStats,
)

@Immutable
data class VersusStats(
    val draws: Int,
    val losses: Int,
    val wins: Int,
    val history: List<VersusStatsGameHistoryItem>,
) {
    companion object {
        val EMPTY = VersusStats(0, 0, 0, emptyList())
    }
}

@Immutable
data class VersusStatsGameHistoryItem(
    val date: Instant,
    val game: Long,
    val state: String,
)