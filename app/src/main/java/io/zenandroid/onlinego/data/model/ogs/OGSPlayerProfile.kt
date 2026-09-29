@file:UseSerializers(
  OGSInstantSerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.local.Player
import java.time.Instant
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSInstantSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer

@Immutable
@Serializable
data class OGSPlayerProfile (
    val user: Player,
    val vs: VersusStats,
)

@Immutable
@Serializable
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
@Serializable
data class VersusStatsGameHistoryItem(
    val date: Instant,
    val game: Long,
    val state: String,
)