package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.runtime.Immutable;
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import java.time.Instant

@Immutable
data class PuzzleDirectoryState (
    val collections: Map<Long, PuzzleCollection> = emptyMap(),
    val recents: Map<Instant, VisitedPuzzleCollection> = emptyMap(),
    val boardTheme: BoardTheme,
    val solutions: Map<Long, Int> = emptyMap(),
)
