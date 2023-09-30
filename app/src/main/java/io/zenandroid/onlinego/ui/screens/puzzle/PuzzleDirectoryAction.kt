package io.zenandroid.onlinego.ui.screens.puzzle

import android.graphics.Point
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection

sealed class PuzzleDirectoryAction {
    object ViewReady: PuzzleDirectoryAction()

    class DataLoadingError(
        val e: Throwable
    ): PuzzleDirectoryAction()

    class PuzzleLoaded(val puzzle: Puzzle): PuzzleDirectoryAction()
    class LoadPuzzle(val id: Long): PuzzleDirectoryAction()
    class WaitPuzzle(val id: Long): PuzzleDirectoryAction()
    class ShowCandidateMove(val placement: Point?): PuzzleDirectoryAction()
    object Finish: PuzzleDirectoryAction()

    // User actions
    class UserTappedCoordinate(val coordinate: Point): PuzzleDirectoryAction()
    class UserHotTrackedCoordinate(val coordinate: Point): PuzzleDirectoryAction()
    object UserPressedPrevious: PuzzleDirectoryAction()
    object UserPressedBack: PuzzleDirectoryAction()
    object UserPressedNext: PuzzleDirectoryAction()
    object UserPressedPass: PuzzleDirectoryAction()
}
