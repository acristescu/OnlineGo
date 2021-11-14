package io.zenandroid.onlinego.ui.screens.localai

import android.util.Log
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.RulesManager.isGameOver
import io.zenandroid.onlinego.mvi.Reducer
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.AIError
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.AIHint
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.AIMove
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.AIOwnershipResponse
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.CantRestoreState
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.DismissNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.EngineStarted
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.EngineStopped
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.EngineWouldNotStart
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.GenerateAiMove
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.HideOwnership
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.NewGame
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.NewPosition
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.PromptUserForMove
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.RestoredState
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ScoreComputed
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ShowNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserAskedForHint
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserAskedForOwnership
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserHotTrackedCoordinate
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedBack
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedNext
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedPass
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedPrevious
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserTappedCoordinate
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserTriedKoMove
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserTriedSuicidalMove
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewPaused
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewReady

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
            is ViewReady -> state.copy(
                    chatText = "Give me a second, I'm getting ready...",
            ).let {
                Log.d("AiGameReducer", "Game Loaded")
                action.loadData?.let { data ->
                    it.copy(
                        position = data.position,
                        boardSize = when(data.position?.boardWidth) {
                            data.position?.boardHeight -> data.position?.boardWidth
                            else -> null //nonsquare
                        }!!,
                        handicap = data.handicap ?: 0,
                        enginePlaysBlack = false,
                        enginePlaysWhite = false,
                        chatText = "Game Loaded!",
                        redoPosStack = emptyList()
                    )
                } ?: it
            }
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
                        newVariation.isGameOver() && state.whiteWon == true -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. White wins!"
                        newVariation.isGameOver() && state.whiteWon == false -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Black wins!"
                        newVariation.isGameOver() && state.whiteWon == null -> "Game ended because of two passes. Hang on, I'm computing the final score."
                        else -> state.chatText
                    },
                    showAiEstimatedTerritory = false,
                    showFinalTerritory = newVariation.isGameOver() && state.whiteWon != null,
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
                    chatText = if (action.whiteWon) "Game ended because of two passes. Final score is black ${action.blackScore} to white ${action.whiteScore}. White wins!"
                               else "Game ended because of two passes. Final score is black ${action.blackScore} to white ${action.whiteScore}. Black wins!",
                    finalWhiteScore = action.whiteScore.toFloat(),
                    finalBlackScore = action.blackScore.toFloat(),
                    whiteWon = action.whiteWon,
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
                val newVariation = if(state.history.lastOrNull() == action.newPos) state.history else state.history + action.newPos
                state.copy(
                    position = action.newPos,
                    history = newVariation,
                    nextButtonEnabled = false,
                    aiAnalysis = action.aiAnalisis,
                    aiQuickEstimation = action.selectedMove,
                    chatText = when {
                        newVariation.isGameOver() && state.whiteWon == true -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. White wins!"
                        newVariation.isGameOver() && state.whiteWon == false -> "Game ended because of two passes. Final score is black ${state.finalBlackScore?.toInt()} to white ${state.finalWhiteScore}. Black wins!"
                        newVariation.isGameOver() && state.whiteWon == null -> "Game ended because of two passes. Hang on, I'm computing the final score."
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
                        state.engineStarted && state.position?.lastMove?.x == -1 -> "Pass! Another pass will conclude the game."
                        state.engineStarted && state.position?.nextToMove == StoneType.WHITE -> "White's turn!"
                        state.engineStarted && state.position?.nextToMove == StoneType.BLACK -> "Black's turn!"
                        else -> state.chatText
                    }
            )
            NextPlayerChanged -> state.copy(
                    boardIsInteractive = !state.boardIsInteractive,
                    passButtonEnabled = !state.passButtonEnabled,
                    previousButtonEnabled = !state.previousButtonEnabled,
                    nextButtonEnabled = !state.nextButtonEnabled
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
                val newPosition = if(aiMovedLast(state) && !aiOnlyGame(state)) state.history.dropLast(2)
                                  else state.history.dropLast(1)
                val removedHistory = if(aiMovedLast(state) && !aiOnlyGame(state)) state.history.takeLast(2)
                                     else state.history.takeLast(1)
                state.copy(
                        position = newHistory.lastOrNull(),
                        redoPosStack = state.redoPosStack + removedHistory,
                        history = newHistory,
                        previousButtonEnabled = newHistory.size > 1,
                        showHints = false,
                        hintButtonVisible = true,
                        ownershipButtonVisible = true,
                        showFinalTerritory = false,
                        showAiEstimatedTerritory = false,
                        nextButtonEnabled = true,
                        boardIsInteractive = true,
                        passButtonEnabled = true,
                        chatText = "Ok, let's try again. Your turn!",
                        whiteWon = null,
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
                    enginePlaysWhite = !action.youPlayWhite,
                    newGameDialogShown = false,
                    showHints = false,
                    whiteWon = null,
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
            ToggleAIBlack -> state.copy(
                    enginePlaysBlack = !state.enginePlaysBlack
            )
            ToggleAIWhite -> state.copy(
                    enginePlaysWhite = !state.enginePlaysWhite
            )
            is EngineWouldNotStart -> state.copy(
                    boardIsInteractive = false,
                    hintButtonVisible = false,
                    ownershipButtonVisible = false,
                    chatText = "Error when starting KataGO: '${action.error.message}'"
            )
        }
    }

    private fun aiMovedLast(state: AiGameState): Boolean =
            (state.position?.lastPlayerToMove == StoneType.BLACK && state.enginePlaysBlack) ||
            (state.position?.lastPlayerToMove == StoneType.WHITE && state.enginePlaysWhite)


    private fun aiOnlyGame(state: AiGameState): Boolean =
            state.enginePlaysBlack || state.enginePlaysWhite
}
