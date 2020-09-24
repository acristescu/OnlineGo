package io.zenandroid.onlinego.ui.screens.localai

import io.zenandroid.onlinego.mvi.Reducer
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*

class AiGameReducer : Reducer<AiGameState, AiGameAction> {
    override fun reduce(state: AiGameState, action: AiGameAction): AiGameState {
        return when(action) {
            EngineStarted -> state.copy(
                    leelaStarted = true
            )
            EngineStopped -> state.copy(leelaStarted = false)
            ViewReady, ViewPaused, UserPressedBack, UserPressedPass  -> {
                state
            }
            is AIAnalysisLine -> state // TODO
            is NewPosition -> state.copy(
                    position = action.newPos,
                    nextButtonEnabled = false
            )
            GenerateAiMove -> state.copy(
                    boardIsInteractive = false,
                    passButtonEnabled = false,
                    previousButtonEnabled = false
            )
            PromptUserForMove -> state.copy(
                    boardIsInteractive = true,
                    passButtonEnabled = true,
                    previousButtonEnabled = state.position?.parentPosition?.parentPosition != null
            )
            is UserHotTrackedCoordinate -> state.copy(
                    candidateMove = action.coordinate
            )
            is UserTappedCoordinate -> state.copy(
                    candidateMove = null
            )
            is EngineLogLine -> state.copy(
                    engineLog = "${action.line}\n${state.engineLog}"
            )
            UserPressedPrevious -> state.copy(
                    position = state.position?.parentPosition?.parentPosition!!,
                    redoPosStack = state.redoPosStack + state.position,
                    previousButtonEnabled = state.position.parentPosition?.parentPosition?.parentPosition?.parentPosition != null,
                    nextButtonEnabled = true
            )
            UserPressedNext -> state.copy(
                    position = state.redoPosStack.last(),
                    redoPosStack = state.redoPosStack.dropLast(1),
                    previousButtonEnabled = true,
                    nextButtonEnabled = state.redoPosStack.size > 1
            )
        }
    }
}