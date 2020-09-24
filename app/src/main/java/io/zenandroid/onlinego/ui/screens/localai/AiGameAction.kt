package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction

sealed class AiGameAction {
    object ViewReady: AiGameAction()
    object ViewPaused: AiGameAction()

    object EngineStarted: AiGameAction()
    object EngineStopped: AiGameAction()

    class EngineLogLine(val line: String): AiGameAction()

    object GenerateAiMove: AiGameAction()
    object PromptUserForMove: AiGameAction()

    class AIAnalysisLine(val line: String): AiGameAction()
    class NewPosition(val newPos: Position): AiGameAction()
//    class UserMove(val newPos: Position): AiGameAction()
//    class AIMove(point: Point, side: StoneType): AiGameAction()
//    class UserMove(point: Point, side: StoneType): AiGameAction()


    // User actions
    class UserTappedCoordinate(val coordinate: Point): AiGameAction()
    class UserHotTrackedCoordinate(val coordinate: Point): AiGameAction()
    object UserPressedPrevious: AiGameAction()
    object UserPressedBack: AiGameAction()
    object UserPressedNext: AiGameAction()
    object UserPressedPass: AiGameAction()

}