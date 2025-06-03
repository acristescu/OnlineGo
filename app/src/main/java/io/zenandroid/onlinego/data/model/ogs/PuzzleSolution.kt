package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable;
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
@Immutable
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
