package io.zenandroid.onlinego.ui.screens.joseki

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition

data class JosekiExplorerState (
        val lastRequestedNodeId: Long? = null,
        val candidateMove: Point? = null,
        val loading: Boolean = false,
        val position: JosekiPosition? = null,
        val description: String? = null,
        val historyStack: List<JosekiPosition> = emptyList(),
        val nextPosStack: List<JosekiPosition> = emptyList(),
        val boardPosition: Position? = null,
        val error: Throwable? = null,
        val shouldFinish: Boolean = false,
        val previousButtonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val passButtonEnabled: Boolean = false
)
