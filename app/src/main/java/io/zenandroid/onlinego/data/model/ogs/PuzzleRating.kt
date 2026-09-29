@file:UseSerializers(LenientIntSerializer::class, LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.UseSerializers

@Entity
@Immutable
@Serializable
data class PuzzleRating (
    @PrimaryKey val puzzleId: Long = -1,
    val rating: Int = 0
)
