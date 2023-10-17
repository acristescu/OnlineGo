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
    @Ignore val player: OGSPlayer? = null,
    val player_rank: Int? = null,
    val player_rating: Int? = null,
    val time_elapsed: Long? = 0,
    val flipped_horizontally: Boolean = false,
    val flipped_vertically: Boolean = false,
    val transposed: Boolean = false,
    val colors_swapped: Boolean = false,
    val attempts: Int? = 0,
    val solution: String = ""
) {
    constructor (
        id: Long?,
        puzzle: Long,
      //player: OGSPlayer?,
        player_rank: Int?,
        player_rating: Int?,
        time_elapsed: Long?,
        flipped_horizontally: Boolean,
        flipped_vertically: Boolean,
        transposed: Boolean,
        colors_swapped: Boolean,
        attempts: Int?,
        solution: String,
    ) : this(
        id = id,
        puzzle = puzzle,
        player = null,
        player_rank = player_rank,
        player_rating = player_rating,
        time_elapsed = time_elapsed,
        flipped_horizontally = flipped_horizontally,
        flipped_vertically = flipped_vertically,
        transposed = transposed,
        colors_swapped = colors_swapped,
        attempts = attempts,
        solution = solution,
    )
}
