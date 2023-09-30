package io.zenandroid.onlinego.ui.screens.puzzle

import android.graphics.Point
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection

sealed class PuzzleSetAction {
    object ViewReady: PuzzleSetAction()

    class DataLoadingError(
        val e: Throwable
    ): PuzzleSetAction()

    class PuzzleLoaded(val puzzle: Puzzle): PuzzleSetAction()
    class LoadPuzzle(val id: Long): PuzzleSetAction()
    class WaitPuzzle(val id: Long): PuzzleSetAction()
    class ShowCandidateMove(val placement: Point?): PuzzleSetAction()
    object Finish: PuzzleSetAction()

    // User actions
    class UserTappedCoordinate(val coordinate: Point): PuzzleSetAction()
    class UserHotTrackedCoordinate(val coordinate: Point): PuzzleSetAction()
    object UserPressedPrevious: PuzzleSetAction()
    object UserPressedBack: PuzzleSetAction()
    object UserPressedNext: PuzzleSetAction()
    object UserPressedPass: PuzzleSetAction()
}
