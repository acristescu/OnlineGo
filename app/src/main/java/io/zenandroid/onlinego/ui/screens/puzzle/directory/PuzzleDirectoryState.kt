package io.zenandroid.onlinego.ui.screens.puzzle.directory

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection

@Immutable
data class PuzzleDirectoryState (
    val collections: List<PuzzleCollection> = emptyList(),
    val recents: Map<Long, VisitedPuzzleCollection> = emptyMap(),
    val recentsPages: List<List<VisitedPuzzleCollection>> = emptyList(),
    val solutions: Map<Long, Int> = emptyMap(),
    val filterString: String? = null,
    val onlyOpenend: Boolean = false,
    val availableSorts: List<PuzzleDirectorySort> = emptyList(),
    val currentSort: PuzzleDirectorySort = PuzzleDirectorySort.RatingSort(false),
    val navigateToPuzzle: Pair<Long, Long>? = null,
)