package io.zenandroid.onlinego.ui.screens.tutorial

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.*

@Immutable
data class TutorialState (
        val tutorialGroups: List<TutorialGroup>? = null,
        val tutorial: Tutorial? = null,
        val step: TutorialStep? = null,
        val node: Node? = null,
        val page: Page? = null,
        val gameExamplePositions: List<Position> = emptyList(),
        val gameExamplePositionIndex: Int = 0,
        val removedStones: List<Pair<Cell, StoneType>>? = null,

        val position: Position? = null,
        val hoveredCell: Cell? = null,
        val text: String? = null,
        val boardInteractive: Boolean = true,
        val retryButtonVisible: Boolean = true,
        val nextButtonVisible: Boolean = false
)