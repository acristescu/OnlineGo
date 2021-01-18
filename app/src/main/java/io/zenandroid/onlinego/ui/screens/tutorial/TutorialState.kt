package io.zenandroid.onlinego.ui.screens.tutorial

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.local.*


data class TutorialState (
        val tutorialGroups: List<TutorialGroup>? = null,
        val tutorial: Tutorial? = null,
        val step: TutorialStep? = null,
        val node: Node? = null,
        val page: Page? = null,

        val position: Position? = null,
        val hoveredCell: Point? = null,
        val text: String? = null,
        val boardInteractive: Boolean = true,
        val retryButtonVisible: Boolean = true,
        val nextButtonVisible: Boolean = false
)