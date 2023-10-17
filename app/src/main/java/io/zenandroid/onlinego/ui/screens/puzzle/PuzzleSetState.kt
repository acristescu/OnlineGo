package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution

@Immutable
data class PuzzleSetState (
    val collection: PuzzleCollection? = null,
    val puzzles: List<Puzzle>? = null,
    val boardTheme: BoardTheme,
    val solutions: Map<Long, Set<PuzzleSolution>> = emptyMap(),
)
