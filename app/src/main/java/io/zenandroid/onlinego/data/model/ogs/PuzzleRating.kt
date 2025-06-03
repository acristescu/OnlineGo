package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
@Immutable
data class PuzzleRating (
    @PrimaryKey val puzzleId: Long = -1,
    val rating: Int = 0
)
