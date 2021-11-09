package io.zenandroid.onlinego.ui.screens.localai

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.MoveInfo

@Immutable
data class AiGameState(
        val engineStarted: Boolean = false,
        val position: Position? = null,
        val history: List<Position> = emptyList(),
        val boardSize: Int = 19,
        val enginePlaysBlack: Boolean = false,
        val enginePlaysWhite: Boolean = false,
        val handicap: Int = 0,
        val boardIsInteractive: Boolean = false,
        val candidateMove: Cell? = null,
        val passButtonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val previousButtonEnabled: Boolean = false,
        val redoPosStack: List<Position> = emptyList(),
        val newGameDialogShown: Boolean = false,
        val chatText: String? = null,
        val showHints: Boolean = false,
        val showAiEstimatedTerritory: Boolean = false,
        val showFinalTerritory: Boolean = false,
        val hintButtonVisible: Boolean = false,
        val ownershipButtonVisible: Boolean = false,
        val finalWhiteScore: Float? = null,
        val finalBlackScore: Float? = null,
        val whiteWon: Boolean? = null,
        val stateRestorePending: Boolean = true,
        val aiAnalysis: Response? = null,
        val aiQuickEstimation: MoveInfo? = null,
)
