package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable;
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle.PuzzleData
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzleCollection
import java.time.Instant

@Entity
@Immutable
data class Puzzle(
    @PrimaryKey val id: Long,
    val order: Int?,
    @Embedded(prefix = "owner_") val owner: Player?,
    val name: String,
    val created: Instant?,
    val modified: Instant?,
    @Embedded(prefix = "puzzle_") val puzzle: PuzzleData,
    val private: Boolean?,
    val width: Int,
    val height: Int,
    val type: String?,
    val has_solution: Boolean?,
    val rating: Float,
    val rating_count: Int,
    val rank: Int,
    @Embedded(prefix = "collection_") val collection: PuzzleCollection?,
    val view_count: Int,
    val solved_count: Int,
    val attempt_count: Int,
) {
    companion object {
        fun fromOGSPuzzle(ogsPuzzle: OGSPuzzle) =
                Puzzle(
                        id = ogsPuzzle.id,
                        order = ogsPuzzle.order,
                        owner = ogsPuzzle.owner?.let(Player::fromOGSPlayer),
                        name = ogsPuzzle.name,
                        created = ogsPuzzle.created,
                        modified = ogsPuzzle.modified,
                        puzzle = ogsPuzzle.puzzle,
                        private = ogsPuzzle.private,
                        width = ogsPuzzle.width,
                        height = ogsPuzzle.height,
                        type = ogsPuzzle.type,
                        has_solution = ogsPuzzle.has_solution,
                        rating = ogsPuzzle.rating,
                        rating_count = ogsPuzzle.rating_count,
                        rank = ogsPuzzle.rank,
                        collection = ogsPuzzle.collection?.let(PuzzleCollection::fromOGSPuzzleCollection),
                        view_count = ogsPuzzle.view_count,
                        solved_count = ogsPuzzle.solved_count,
                        attempt_count = ogsPuzzle.attempt_count,
                )
    }
}
