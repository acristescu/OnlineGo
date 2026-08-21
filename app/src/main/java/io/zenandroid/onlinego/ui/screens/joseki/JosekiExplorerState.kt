package io.zenandroid.onlinego.ui.screens.joseki

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition

/**
 * The markdown shown above the board. It is either supplied by the server for the current position
 * or, for the root position, our own explanation of the screen - which the UI resolves, so that the
 * view model does not need a Context.
 */
@Immutable
sealed interface JosekiDescription {
        data class Markdown(val markdown: String) : JosekiDescription
        data class FromResource(@StringRes val resId: Int) : JosekiDescription
}

@Immutable
data class JosekiExplorerState (
        val lastRequestedNodeId: Long? = null,
        val candidateMove: Cell? = null,
        val loading: Boolean = false,
        val position: JosekiPosition? = null,
        val description: JosekiDescription? = null,
        val historyStack: List<JosekiPosition> = emptyList(),
        val nextPosStack: List<JosekiPosition> = emptyList(),
        val boardPosition: Position? = null,
        val error: Throwable? = null,
        val shouldFinish: Boolean = false,
        val previousButtonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val passButtonEnabled: Boolean = false
)
