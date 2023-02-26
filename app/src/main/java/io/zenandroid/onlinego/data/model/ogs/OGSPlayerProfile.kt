package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.model.local.Player
import org.threeten.bp.Instant

data class OGSPlayerProfile (
    val user: Player,
    val vs: VersusStats,
)

data class VersusStats(
    val draws: Int,
    val losses: Int,
    val wins: Int,
    val history: List<VersusStatsGameHistoryItem>,
)

data class VersusStatsGameHistoryItem(
    val date: Instant,
    val game: Long,
    val state: String,
)