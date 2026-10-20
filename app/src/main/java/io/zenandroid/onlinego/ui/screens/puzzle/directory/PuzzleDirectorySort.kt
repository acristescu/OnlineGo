package io.zenandroid.onlinego.ui.screens.puzzle.directory

import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.puzzle_directory_sort_count
import io.zenandroid.onlinego.resources.puzzle_directory_sort_name
import io.zenandroid.onlinego.resources.puzzle_directory_sort_rank
import io.zenandroid.onlinego.resources.puzzle_directory_sort_rating
import io.zenandroid.onlinego.resources.puzzle_directory_sort_views
import org.jetbrains.compose.resources.StringResource

sealed class PuzzleDirectorySort(
    val asc: Boolean
) {
  val desc: Boolean
    get() = !asc

  abstract val comparator: Comparator<PuzzleCollection?>
  abstract val reversed: PuzzleDirectorySort

  abstract val labelResId: StringResource

  class NameSort(asc: Boolean = true) : PuzzleDirectorySort(asc) {
    override val comparator: Comparator<PuzzleCollection?> = if (asc) compareBy { it?.name }
    else compareByDescending { it?.name }

    override val reversed: PuzzleDirectorySort
      get() = NameSort(!asc)

    override val labelResId = Res.string.puzzle_directory_sort_name
  }

  class RatingSort(asc: Boolean = true) : PuzzleDirectorySort(asc) {
    override val comparator: Comparator<PuzzleCollection?> = if (asc) compareBy<PuzzleCollection?> { it?.rating }
        .thenByDescending { it?.rating_count }
    else compareByDescending<PuzzleCollection?> { it?.rating }
        .thenByDescending { it?.rating_count }

    override val reversed: PuzzleDirectorySort
      get() = RatingSort(!asc)

    override val labelResId = Res.string.puzzle_directory_sort_rating
  }

  class CountSort(asc: Boolean = true) : PuzzleDirectorySort(asc) {
    override val comparator: Comparator<PuzzleCollection?> = if (asc) compareBy { it?.puzzle_count }
    else compareByDescending { it?.puzzle_count }

    override val reversed: PuzzleDirectorySort
      get() = CountSort(!asc)

    override val labelResId = Res.string.puzzle_directory_sort_count
  }

  class ViewsSort(asc: Boolean = true) : PuzzleDirectorySort(asc) {
    override val comparator: Comparator<PuzzleCollection?> = if (asc) compareBy { it?.view_count }
    else compareByDescending { it?.view_count }

    override val reversed: PuzzleDirectorySort
      get() = ViewsSort(!asc)

    override val labelResId = Res.string.puzzle_directory_sort_views
  }

  class RankSort(asc: Boolean = true) : PuzzleDirectorySort(asc) {
    override val comparator: Comparator<PuzzleCollection?> = if (asc) compareBy({ it?.min_rank }, { it?.max_rank })
    else compareByDescending<PuzzleCollection?> { it?.min_rank }
        .thenByDescending { it?.max_rank }

    override val reversed: PuzzleDirectorySort
      get() = RankSort(!asc)

    override val labelResId = Res.string.puzzle_directory_sort_rank
  }
}
