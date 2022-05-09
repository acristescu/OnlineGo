package io.zenandroid.onlinego.ui.screens.localai

import android.util.Log
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.RulesManager.isGameOver
import io.zenandroid.onlinego.mvi.Reducer
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*

class AiGameReducer : Reducer<AiGameState, AiGameAction> {
    override fun reduce(state: AiGameState, action: AiGameAction): AiGameState {
        Log.v("AiGame", "reduce action = $action")
        return when(action) {
            EngineStarted -> state.copy(
                    engineStarted = true,
                    chatText = when {
                        state.position == null && state.newGameDialogShown -> "Ready!"
                        state.position == null && !state.newGameDialogShown -> "Use the 'New Game' button to start a new game"
                        else -> state.chatText
                    }
            )
            EngineStopped -> state.copy(engineStarted = false)
            ViewPaused, UserPressedBack, UserPressedPass  -> {
                state
            }
            ViewReady -> state.copy(
                    chatText = "Give me a second, I'm getting ready..."
            )
            is NewPosition -> {
                val newVariation = if(state.history.lastOrNull() == action.newPos) state.history else state.history + action.newPos
                state.copy(
                    position = action.newPos,
                    history = newVariation,
                    nextButtonEnabled = false,
                    redoPosStack = emptyList(),
                    boardIsInteractive = false,
                    showHints = false,
                    chatText = when {
                        newVariation.isGameOver() && state.aiWon == true -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Looks like I win this time."
                        newVariation.isGameOver() && state.aiWon == false -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Congrats, looks like you got the better of me."
                        newVariation.isGameOver() && state.aiWon == null -> "Game ended because of two passes. Hang on, I'm computing the final score."
                        else -> state.chatText
                    },
                    showAiEstimatedTerritory = false,
                    showFinalTerritory = newVariation.isGameOver() && state.aiWon != null,
                    hintButtonVisible = !newVariation.isGameOver(),
                    ownershipButtonVisible = !newVariation.isGameOver()
                )
            }
            is ScoreComputed -> state.copy(
                    position = action.newPos,
                    history = state.history.dropLast(1) + action.newPos,
                    nextButtonEnabled = false,
                    passButtonEnabled = false,
                    redoPosStack = emptyList(),
                    boardIsInteractive = false,
                    chatText = if (action.aiWon) "Game ended because of two passes. Final score is black ${action.blackScore} to white ${action.whiteScore}. Looks like I win this time."
                                else "Game ended because of two passes. Final score is black ${action.blackScore} to white ${action.whiteScore}. Congrats, looks like you got the better of me.",
                    finalWhiteScore = action.whiteScore,
                    finalBlackScore = action.blackScore.toFloat(),
                    aiWon = action.aiWon,
                    previousButtonEnabled = true,
                    showAiEstimatedTerritory = false,
                    showFinalTerritory = true,
                    hintButtonVisible = false,
                    ownershipButtonVisible = false,
                    showHints = false,
                    candidateMove = null,
                    aiAnalysis = action.aiAnalisis,
            )
            is AIMove -> {
                val newVariation = state.history + action.newPos
                state.copy(
                    position = action.newPos,
                    history = newVariation,
                    nextButtonEnabled = false,
                    aiAnalysis = action.aiAnalisis,
                    aiQuickEstimation = action.selectedMove,
                    chatText = when {
                        newVariation.isGameOver() && state.aiWon == true -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Looks like I win this time."
                        newVariation.isGameOver() && state.aiWon == false -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Congrats, looks like you got the better of me."
                        newVariation.isGameOver() && state.aiWon == null -> "Game ended because of two passes. Hang on, I'm computing the final score."
                        else -> state.chatText
                    }
                )
            }
            GenerateAiMove -> state.copy(
                    boardIsInteractive = false,
                    passButtonEnabled = false,
                    previousButtonEnabled = false,
                    nextButtonEnabled = false,
                    chatText = "I'm thinking..."
            )
            PromptUserForMove -> state.copy(
                    boardIsInteractive = true,
                    passButtonEnabled = true,
                    previousButtonEnabled = state.history.size > 2,
                    chatText = when {
                        state.engineStarted && state.position?.lastMove?.x == -1 -> "Pass! If you agree the game is over you should pass as well."
                        state.position != null && state.engineStarted -> "Your turn!"
                        else -> state.chatText
                    }
            )
            is UserHotTrackedCoordinate -> state.copy(
                    candidateMove = action.coordinate
            )
            is UserTappedCoordinate -> state.copy(
                    candidateMove = null
            )
            is UserTriedKoMove -> state.copy(
                    candidateMove = null,
                    chatText = "That is an illegal KO move. Repeating a position is not allowed. Try again!"
            )
            is UserTriedSuicidalMove -> state.copy(
                    candidateMove = null,
                    chatText = "That move is illegal because you would kill your own group. Try again!"
            )
            AIError -> state.copy(
                    chatText = "An error occurred communicating with the AI"
            )
            UserPressedPrevious -> {
                val newHistory = state.history.dropLast(2)
                state.copy(
                        position = newHistory.lastOrNull(),
                        redoPosStack = state.redoPosStack + state.history.takeLast(2),
                        history = newHistory,
                        previousButtonEnabled = newHistory.size > 2,
                        showHints = false,
                        hintButtonVisible = true,
                        ownershipButtonVisible = true,
                        showFinalTerritory = false,
                        showAiEstimatedTerritory = false,
                        nextButtonEnabled = true,
                        boardIsInteractive = true,
                        passButtonEnabled = true,
                        chatText = "Ok, let's try again. Your turn!",
                        aiWon = null,
                        finalBlackScore = null,
                        finalWhiteScore = null
                )
            }
            UserPressedNext -> {
                val newHistory = state.history + state.redoPosStack.takeLast(2)
                state.copy(
                    position = newHistory.lastOrNull(),
                    history = newHistory,
                    redoPosStack = state.redoPosStack.dropLast(2),
                    previousButtonEnabled = true,
                    showHints = false,
                    nextButtonEnabled = state.redoPosStack.size > 2
                )
            }
            ShowNewGameDialog -> state.copy(
                    newGameDialogShown = true
            )
            DismissNewGameDialog -> state.copy(
                    newGameDialogShown = false,
                    chatText = if(state.position == null) "Use the 'New Game' button to start a new game" else state.chatText
            )
            is NewGame -> state.copy(
                    boardSize = action.size,
                    handicap = action.handicap,
                    enginePlaysBlack = !action.youPlayBlack,
                    newGameDialogShown = false,
                    showHints = false,
                    aiWon = null,
                    finalWhiteScore = null,
                    finalBlackScore = null,
                    showFinalTerritory = false,
                    hintButtonVisible = true,
                    ownershipButtonVisible = true,
                    showAiEstimatedTerritory = false,
                    nextButtonEnabled = false,
                    passButtonEnabled = false,
                    chatText = "",
                    previousButtonEnabled = false,
                    boardIsInteractive = false,
                    redoPosStack = emptyList(),
                    candidateMove = null,
                    history = emptyList(),
                    position = RulesManager.initializePosition(action.size, action.handicap),
            )
            is AIHint -> state.copy(
                    showHints = true,
                    aiAnalysis = action.aiAnalisis,
                    chatText = "Here are a few moves to consider"
            )
            UserAskedForHint -> state.copy(
                    chatText = "Hmmm..."
            )
            is RestoredState -> action.state.copy( // Careful, this stomps on everything not in the list below!!!
                    engineStarted = state.engineStarted,
                    stateRestorePending = false,
            )
            CantRestoreState -> state.copy(
                    newGameDialogShown = true,
                    stateRestorePending = false,
            )
            is AIOwnershipResponse -> state.copy(
                    boardIsInteractive = true,
                    aiAnalysis = action.aiAnalisis,
                    showAiEstimatedTerritory = true,
                    chatText = "Here's what I think the territories look like"
            )
            UserAskedForOwnership -> state.copy(
                    boardIsInteractive = false,
                    chatText = "Ok, calculating current territory..."
            )
            HideOwnership -> state.copy(
                    showAiEstimatedTerritory = false,
                    chatText = "Ok, your turn",
                    boardIsInteractive = true
            )
            is EngineWouldNotStart -> state.copy(
                    boardIsInteractive = false,
                    hintButtonVisible = false,
                    ownershipButtonVisible = false,
                    chatText = "Error when starting KataGO: '${action.error.message}'"
            )
        }
    }
}