package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable;
import androidx.room.Embedded
import io.zenandroid.onlinego.data.model.local.InitialState
import java.time.Instant

@Immutable
data class MoveTree (
    val y: Int = -1,
    val x: Int = -1,
    val correct_answer: Boolean? = null,
    val wrong_answer: Boolean? = null,
    val text: String? = null,
    val branches: List<MoveTree>? = null,
    val marks: List<Mark>? = null,
    val pen_marks: List<PenData>? = null
) {
    @Immutable
    data class Mark (
        val y: Int,
        val x: Int,
        val marks: MarkData,
    ) {
        @Immutable
        data class MarkData (
            val letter: String?,
            val transient_letter: String?,
            val subscript: String?,
            val color: String?,
          //val score: String?, // or bool
            val triangle: Boolean = false,
            val square: Boolean = false,
            val circle: Boolean = false,
            val cross: Boolean = false,
          //val blue_move: Boolean = false,
            val chat_triangle: Boolean = false,
            val sub_triangle: Boolean = false,
            val remove: Boolean = false,
            val stone_removed: Boolean = false,
            val mark_x: Boolean = false,
            val hint: Boolean = false,
            val black: Boolean?,
            val white: Boolean?,
        ) {
            override fun toString(): String {
                return letter ?: transient_letter ?: subscript ?: color
                ?: when {
                    triangle || chat_triangle || sub_triangle -> "△"
                    square -> "□"
                    circle -> "○"
                    cross -> "⨯"
                    else -> ""
                }
            }
        }
    }

    @Immutable
    data class PenData (
        val color: String?,
        val points: List<Int>?
    )
}

data class OGSPuzzle (
    var id: Long = -1,
    var order: Int? = -1,
    var owner: OGSPlayer? = null,
    var name: String = "",
    var created: Instant? = null,
    var modified: Instant? = null,
    var puzzle: PuzzleData = PuzzleData(),
    var private: Boolean? = null,
    var width: Int = 0,
    var height: Int = 0,
    var type: String? = null,
    var has_solution: Boolean? = null,
    var rating: Float = 0f,
    var rating_count: Int = 0,
    var rank: Int = 0,
    var collection: OGSPuzzleCollection? = null,
    var view_count: Int = 0,
    var solved_count: Int = 0,
    var attempt_count: Int = 0,
) {
    data class PuzzleData (
        var puzzle_rank: String = "",
        var name: String = "",
        var move_tree: MoveTree = MoveTree(),
        var initial_player: String = "",
        var height: Int = 0,
        var width: Int = 0,
        var mode: String = "",
        var puzzle_collection: String = "",
        var puzzle_type: String = "",
        @Embedded(prefix = "initial_state_") var initial_state: InitialState = InitialState(),
        var puzzle_description: String = ""
    )
}
