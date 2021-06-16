package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction
import org.koin.core.context.GlobalContext.get

data class AiGameState(
        val engineStarted: Boolean = false,
        val position: Position? = null,
        val boardSize: Int = 19,
        val enginePlaysBlack: Boolean = false,
        val handicap: Int = 0,
        val boardIsInteractive: Boolean = false,
        val candidateMove: Point? = null,
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
        val aiWon: Boolean? = null
)