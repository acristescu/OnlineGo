package io.zenandroid.onlinego.joseki

import android.graphics.Point
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.ogs.JosekiPosition

data class JosekiExplorerState (
        val lastRequestedNodeId: Long? = null,
        val candidateMove: Point? = null,
        val loading: Boolean = false,
        val position: JosekiPosition? = null,
        val boardPosition: Position? = null,
        val error: Throwable? = null
)
