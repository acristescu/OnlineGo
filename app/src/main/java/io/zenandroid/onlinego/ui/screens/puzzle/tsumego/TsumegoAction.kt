package io.zenandroid.onlinego.ui.screens.puzzle.tsumego

import android.graphics.Point
import io.zenandroid.onlinego.data.model.local.Puzzle

sealed interface TsumegoAction {
    data object ViewReady: TsumegoAction

    data class DataLoadingError(
        val e: Throwable
    ): TsumegoAction

    data class PuzzleLoaded(val puzzle: Puzzle): TsumegoAction
    data class LoadPuzzle(val id: Long): TsumegoAction
    data class WaitPuzzle(val id: Long): TsumegoAction
    data class ShowCandidateMove(val placement: Point?): TsumegoAction
    data object Finish: TsumegoAction

    // User actions
    data class UserTappedCoordinate(val coordinate: Point): TsumegoAction
    data class UserHotTrackedCoordinate(val coordinate: Point): TsumegoAction
    data object UserPressedPrevious: TsumegoAction
    data object UserPressedBack: TsumegoAction
    data object UserPressedNext: TsumegoAction
    data object UserPressedPass: TsumegoAction
    data class BoardCellHovered(val point: Point): TsumegoAction
    data class BoardCellTapped(val point: Point): TsumegoAction
    data object RetryPressed: TsumegoAction
    data object NextPressed: TsumegoAction
}
