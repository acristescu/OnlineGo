package io.zenandroid.onlinego.ui.screens.tutorial

import io.zenandroid.onlinego.data.model.Cell

sealed class TutorialAction {
  data class BoardCellHovered(val point: Cell) : TutorialAction()
  data class BoardCellTapped(val point: Cell) : TutorialAction()
  object RetryPressed : TutorialAction()
  object NextPressed : TutorialAction()
}