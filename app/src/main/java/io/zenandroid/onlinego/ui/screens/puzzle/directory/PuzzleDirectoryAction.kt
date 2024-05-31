package io.zenandroid.onlinego.ui.screens.puzzle.directory

import android.graphics.Point

sealed class PuzzleDirectoryAction {
  object ViewReady : PuzzleDirectoryAction()

  class DataLoadingError(
      val e: Throwable
  ) : PuzzleDirectoryAction()

  // User actions
  class UserTappedCoordinate(val coordinate: Point) : PuzzleDirectoryAction()
  class UserHotTrackedCoordinate(val coordinate: Point) : PuzzleDirectoryAction()
  object UserPressedPrevious : PuzzleDirectoryAction()
  object UserPressedBack : PuzzleDirectoryAction()
  object UserPressedNext : PuzzleDirectoryAction()
  object UserPressedPass : PuzzleDirectoryAction()
}
