package io.zenandroid.onlinego.ui.screens.tutorial

import android.graphics.Point

sealed class TutorialAction {
    sealed class HandledByFragment: TutorialAction() {
        object BackArrowPressed : HandledByFragment()
    }

    sealed class HandledByViewModel: TutorialAction() {
        data class BoardCellHovered(val point: Point) : HandledByViewModel()
        data class BoardCellTapped(val point: Point) : HandledByViewModel()
        object RetryPressed : HandledByViewModel()
        object NextPressed : HandledByViewModel()
    }

}