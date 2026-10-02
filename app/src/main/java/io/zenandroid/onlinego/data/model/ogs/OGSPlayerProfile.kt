@file:UseSerializers(
  OGSInstantSerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import io.zenandroid.onlinego.data.ogs.OGSInstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Instant

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