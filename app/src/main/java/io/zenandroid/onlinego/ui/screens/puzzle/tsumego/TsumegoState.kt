package io.zenandroid.onlinego.ui.screens.puzzle.tsumego

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.model.StoneType
import java.time.Instant
import java.util.Stack

@Immutable
data class TsumegoState (
        val puzzle: Puzzle? = null,
        val collection: PuzzleCollection? = null,
        val description: String = "",
        val rating: PuzzleRating? = null,
        val solutions: List<PuzzleSolution> = emptyList(),
        val startTime: Instant? = null,
        val attemptCount: Int = 0,
        val sgfMoves: String = "",
        val candidateMove: Cell? = null,
        val boardPosition: Position? = null,
        val previousButtonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val passButtonEnabled: Boolean = false,
        val nodeStack: Stack<MoveTree?> = Stack(),
        val removedStones: Map<Cell, StoneType>? = null,
        val hoveredCell: Cell? = null,
        val boardInteractive: Boolean = true,
        val retryButtonVisible: Boolean = false,
        val continueButtonVisible: Boolean = false
)
