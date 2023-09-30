package io.zenandroid.onlinego.ui.screens.puzzle

import io.zenandroid.onlinego.data.model.local.PuzzleCollection

sealed class PuzzleDirectorySort (
    val asc: Boolean
) {
    val desc: Boolean
        get() = !asc

    abstract val compare: Comparator<PuzzleCollection>
    abstract val reversed: PuzzleDirectorySort

    class NameSort(asc: Boolean = true): PuzzleDirectorySort(asc) {
        override val compare: Comparator<PuzzleCollection>
            = if(asc) compareBy({ it.name })
              else compareByDescending({ it.name })
        override val reversed: PuzzleDirectorySort
            get() = NameSort(!asc)
    }
    class RatingSort(asc: Boolean = true): PuzzleDirectorySort(asc) {
        override val compare: Comparator<PuzzleCollection>
            = if(asc) compareBy<PuzzleCollection>({ it.rating })
                    .thenByDescending({ it.rating_count })
              else compareByDescending<PuzzleCollection>({ it.rating })
                    .thenByDescending({ it.rating_count })
        override val reversed: PuzzleDirectorySort
            get() = RatingSort(!asc)
    }
    class CountSort(asc: Boolean = true): PuzzleDirectorySort(asc) {
        override val compare: Comparator<PuzzleCollection>
            = if(asc) compareBy({ it.puzzle_count })
              else compareByDescending({ it.puzzle_count })
        override val reversed: PuzzleDirectorySort
            get() = CountSort(!asc)
    }
    class ViewsSort(asc: Boolean = true): PuzzleDirectorySort(asc) {
        override val compare: Comparator<PuzzleCollection>
            = if(asc) compareBy({ it.view_count })
              else compareByDescending({ it.view_count })
        override val reversed: PuzzleDirectorySort
            get() = ViewsSort(!asc)
    }
    class RankSort(asc: Boolean = true): PuzzleDirectorySort(asc) {
        override val compare: Comparator<PuzzleCollection>
            = if(asc) compareBy({ it.min_rank }, { it.max_rank })
              else compareByDescending<PuzzleCollection>({ it.min_rank })
                    .thenByDescending({ it.max_rank })
        override val reversed: PuzzleDirectorySort
            get() = RankSort(!asc)
    }
}
