package io.zenandroid.onlinego.ui.screens.localai

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.local.SgfData

sealed class AiGameAction {
    class ViewReady(val loadData: SgfData?, val savedData: String?): AiGameAction()
    object ViewPaused: AiGameAction()
    class RestoredState(val state: AiGameState): AiGameAction()
    object CantRestoreState: AiGameAction()

    object ShowNewGameDialog: AiGameAction()
    object DismissNewGameDialog: AiGameAction()
    class NewGame(
            val size: Int,
            val youPlayBlack: Boolean,
            val youPlayWhite: Boolean,
            val handicap: Int
    ): AiGameAction()

    object EngineStarted: AiGameAction()
    class EngineWouldNotStart(val error: Throwable): AiGameAction()
    object EngineStopped: AiGameAction()

    object GenerateAiMove: AiGameAction()
    object PromptUserForMove: AiGameAction()

    class NewPosition(val newPos: Position): AiGameAction()
    class AIMove(val newPos: Position, val aiAnalisis: Response, val selectedMove: MoveInfo): AiGameAction()
    object NextPlayerChanged: AiGameAction()
    object AIError: AiGameAction()
    class AIHint(val aiAnalisis: Response): AiGameAction()
    class AIOwnershipResponse(val aiAnalisis: Response): AiGameAction()
    object HideOwnership: AiGameAction()
    class ScoreComputed(val newPos: Position, val whiteScore: Float, val blackScore: Int, val whiteWon: Boolean, val aiAnalisis: Response): AiGameAction()


    // User actions
    class UserTappedCoordinate(val coordinate: Cell): AiGameAction()
    class UserHotTrackedCoordinate(val coordinate: Cell): AiGameAction()
    object UserPressedPrevious: AiGameAction()
    object UserPressedBack: AiGameAction()
    object UserPressedNext: AiGameAction()
    object UserPressedPass: AiGameAction()
    object UserAskedForHint: AiGameAction()
    object UserAskedForOwnership: AiGameAction()
    class UserTriedSuicidalMove(val coordinate: Cell): AiGameAction()
    class UserTriedKoMove(val coordinate: Cell): AiGameAction()
    object ToggleAIBlack: AiGameAction()
    object ToggleAIWhite: AiGameAction()

}
