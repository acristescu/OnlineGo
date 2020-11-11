package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position

sealed class AiGameAction {
    object ViewReady: AiGameAction()
    object ViewPaused: AiGameAction()
    class RestoredState(val state: AiGameState): AiGameAction()

    object ShowNewGameDialog: AiGameAction()
    object DismissNewGameDialog: AiGameAction()
    class NewGame(
            val size: Int,
            val youPlayBlack: Boolean,
            val handicap: Int
    ): AiGameAction()

    object EngineStarted: AiGameAction()
    class EngineWouldNotStart(val error: Throwable): AiGameAction()
    object EngineStopped: AiGameAction()

    object GenerateAiMove: AiGameAction()
    object PromptUserForMove: AiGameAction()

    class NewPosition(val newPos: Position): AiGameAction()
    class AIMove(val newPos: Position): AiGameAction()
    object AIError: AiGameAction()
    object AIHint: AiGameAction()
    object AIOwnershipResponse: AiGameAction()
    object HideOwnership: AiGameAction()
    class ScoreComputed(val newPos: Position, val whiteScore: Float, val blackScore: Int, val aiWon: Boolean): AiGameAction()


    // User actions
    class UserTappedCoordinate(val coordinate: Point): AiGameAction()
    class UserHotTrackedCoordinate(val coordinate: Point): AiGameAction()
    object UserPressedPrevious: AiGameAction()
    object UserPressedBack: AiGameAction()
    object UserPressedNext: AiGameAction()
    object UserPressedPass: AiGameAction()
    object UserAskedForHint: AiGameAction()
    object UserAskedForOwnership: AiGameAction()
    class UserTriedSuicidalMove(val coordinate: Point): AiGameAction()
    class UserTriedKoMove(val coordinate: Point): AiGameAction()


}