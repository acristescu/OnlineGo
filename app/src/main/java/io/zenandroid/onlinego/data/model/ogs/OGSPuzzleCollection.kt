package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable;
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import java.time.Instant

@Immutable
data class OGSPuzzleCollection (
    val id: Long = -1,
    val owner: OGSPlayer? = null,
    val name: String = "",
    val created: Instant? = null,
    @ColumnInfo(name = "is_private") val private: Boolean = false,
    val price: String? = null,
    val starting_puzzle: StartingPuzzle = StartingPuzzle(),
    val rating: Float = 0f,
    val rating_count: Int = 0,
    val puzzle_count: Int = 0,
    val min_rank: Int = 0,
    val max_rank: Int = 0,
    val view_count: Int = 0,
    val solved_count: Int = 0,
    val attempt_count: Int = 0,
    val color_transform_enabled: Boolean? = null,
    val position_transform_enabled: Boolean? = null,
) {
    @Immutable
    data class StartingPuzzle (
        val id: Long = -1,
        @Embedded(prefix = "initial_state_") val initial_state: InitialState = InitialState(),
        val width: Int = 0,
        val height: Int = 0
    )
}
