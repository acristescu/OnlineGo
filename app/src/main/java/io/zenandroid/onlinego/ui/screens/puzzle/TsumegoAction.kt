package io.zenandroid.onlinego.ui.screens.puzzle

import android.graphics.Point
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection

sealed class TsumegoAction {
    object ViewReady: TsumegoAction()

    class DataLoadingError(
        val e: Throwable
    ): TsumegoAction()

    class PuzzleLoaded(val puzzle: Puzzle): TsumegoAction()
    class LoadPuzzle(val id: Long): TsumegoAction()
    class WaitPuzzle(val id: Long): TsumegoAction()
    class ShowCandidateMove(val placement: Point?): TsumegoAction()
    object Finish: TsumegoAction()

    // User actions
    class UserTappedCoordinate(val coordinate: Point): TsumegoAction()
    class UserHotTrackedCoordinate(val coordinate: Point): TsumegoAction()
    object UserPressedPrevious: TsumegoAction()
    object UserPressedBack: TsumegoAction()
    object UserPressedNext: TsumegoAction()
    object UserPressedPass: TsumegoAction()
    data class BoardCellHovered(val point: Point): TsumegoAction()
    data class BoardCellTapped(val point: Point): TsumegoAction()
    object RetryPressed: TsumegoAction()
    object NextPressed: TsumegoAction()
}
