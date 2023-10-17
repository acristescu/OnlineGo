package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable;
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzleCollection.StartingPuzzle
import java.time.Instant

@Entity
@Immutable
data class PuzzleCollection(
    @PrimaryKey val id: Long,
    @Embedded(prefix = "owner_") val owner: Player?,
    val name: String,
    val created: Instant?,
    @ColumnInfo(name = "is_private") val private: Boolean,
    val price: String?,
    @Embedded(prefix = "starting_puzzle_") val starting_puzzle: StartingPuzzle,
    val rating: Float,
    val rating_count: Int,
    val puzzle_count: Int,
    val min_rank: Int,
    val max_rank: Int,
    val view_count: Int,
    val solved_count: Int,
    val attempt_count: Int,
    val color_transform_enabled: Boolean?,
    val position_transform_enabled: Boolean?,
) {
    companion object {
        fun fromOGSPuzzleCollection(ogsPuzzleCollection: OGSPuzzleCollection) =
                PuzzleCollection(
                        id = ogsPuzzleCollection.id,
                        owner = ogsPuzzleCollection.owner?.let(Player::fromOGSPlayer),
                        name = ogsPuzzleCollection.name,
                        created = ogsPuzzleCollection.created,
                        private = ogsPuzzleCollection.private,
                        price = ogsPuzzleCollection.price,
                        starting_puzzle = ogsPuzzleCollection.starting_puzzle,
                        rating = ogsPuzzleCollection.rating,
                        rating_count = ogsPuzzleCollection.rating_count,
                        puzzle_count = ogsPuzzleCollection.puzzle_count,
                        min_rank = ogsPuzzleCollection.min_rank,
                        max_rank = ogsPuzzleCollection.max_rank,
                        view_count = ogsPuzzleCollection.view_count,
                        solved_count = ogsPuzzleCollection.solved_count,
                        attempt_count = ogsPuzzleCollection.attempt_count,
                        color_transform_enabled = ogsPuzzleCollection.color_transform_enabled,
                        position_transform_enabled = ogsPuzzleCollection.position_transform_enabled,
                )
    }
}
