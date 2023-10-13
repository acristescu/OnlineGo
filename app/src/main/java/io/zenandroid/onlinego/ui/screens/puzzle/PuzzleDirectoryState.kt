package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.runtime.Immutable;
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import java.time.Instant
import java.util.SortedMap

@Immutable
data class PuzzleDirectoryState (
    val collections: SortedMap<Long, PuzzleCollection> = sortedMapOf(),
    val recents: Map<Long, VisitedPuzzleCollection> = emptyMap(),
    val recentsPages: List<List<VisitedPuzzleCollection>> = emptyList(),
    val boardTheme: BoardTheme,
    val solutions: Map<Long, Int> = emptyMap(),
)
