@file:UseSerializers(
    OGSBooleanSerializer::class,
    LenientIntSerializer::class,
    LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable;
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer

@Entity
@Immutable
@Serializable
data class PuzzleSolution (
    @PrimaryKey val id: Long? = null,
    val puzzle: Long = -1,
    val player_rank: Int? = null,
    val player_rating: Int? = null,
    val time_elapsed: Long? = 0,
    val flipped_horizontally: Boolean = false,
    val flipped_vertically: Boolean = false,
    val transposed: Boolean = false,
    val colors_swapped: Boolean = false,
    val attempts: Int? = 0,
    val solution: String = ""
)
