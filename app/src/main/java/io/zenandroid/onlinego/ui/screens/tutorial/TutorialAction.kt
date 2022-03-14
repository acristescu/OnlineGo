package io.zenandroid.onlinego.ui.screens.tutorial

import io.zenandroid.onlinego.data.model.Cell

sealed class TutorialAction {
    sealed class HandledByFragment: TutorialAction() {
        object BackArrowPressed : HandledByFragment()
    }

    sealed class HandledByViewModel: TutorialAction() {
        data class BoardCellHovered(val point: Cell) : HandledByViewModel()
        data class BoardCellTapped(val point: Cell) : HandledByViewModel()
        object RetryPressed : HandledByViewModel()
        object NextPressed : HandledByViewModel()
    }

}