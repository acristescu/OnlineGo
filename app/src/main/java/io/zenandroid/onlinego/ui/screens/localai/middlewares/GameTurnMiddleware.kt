package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.util.Log
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.RulesManager.isGameOver
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.zenandroid.onlinego.utils.recordException

class GameTurnMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            Observable.merge(
                    engineStarted(actions, state),
                    newGame(actions),
                    nextMove(actions, state),
                    computeScore(actions, state)
            ).mergeWith( toggleAI(actions, state) )

    private fun engineStarted(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.filter { it is EngineStarted || it is RestoredState }
                .withLatestFrom(state)
                    .filter { (_, state) -> !state.stateRestorePending && state.engineStarted && state.position != null && !(state.history.isGameOver() && state.whiteWon != null) }
                    .map { (_, state) -> NewPosition(state.position!!) }

    private fun newGame(actions: Observable<AiGameAction>) =
            actions.ofType(NewGame::class.java)
                    .map { NewPosition(RulesManager.initializePosition(it.size, it.handicap)) }

    private fun nextMove(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.filter { it is NewPosition || it is AIMove || it is NextPlayerChanged || it is DismissNewGameDialog }
                    .withLatestFrom(state)
                    .filter { (_, state) -> !state.history.isGameOver() && !state.newGameDialogShown }
                    .map { (_, state) ->
                        if ((state.position?.nextToMove == StoneType.BLACK && state.enginePlaysBlack) ||
                            (state.position?.nextToMove == StoneType.WHITE && state.enginePlaysWhite)) {
                            GenerateAiMove
                        } else {
                            PromptUserForMove
                        }
                    }

    private fun computeScore(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.filter { it is NewPosition || it is AIMove }
                    .withLatestFrom(state)
                    .filter { (_, state) -> state.history.isGameOver() }
                    .flatMap { (_, state) ->
                        KataGoAnalysisEngine.analyzeMoveSequence(
                                sequence = state.history,
                                maxVisits = 10,
                                komi = state.position!!.komi,
                                includeOwnership = true
                        )
                                .map {
                                    val blackTerritory = mutableSetOf<Cell>()
                                    val whiteTerritory = mutableSetOf<Cell>()
                                    val removedSpots = mutableSetOf<Cell>()

                                    for(i in 0 until state.position.boardWidth) {
                                        for(j in 0 until state.position.boardHeight) {
                                            val p = Cell(i, j)
                                            val ownership = it.ownership!![j*state.position.boardWidth + i] // a float between -1 and 1, -1 is 100% solid black territory, 1 is 100% solid white territory
                                            when {
                                                state.position.whiteStones.contains(p) && ownership < 0 -> {
                                                    blackTerritory += p
                                                    removedSpots += p
                                                }
                                                state.position.blackStones.contains(p) && ownership > 0 -> {
                                                    whiteTerritory += p
                                                    removedSpots += p
                                                }
                                                ownership > 0.6 && !state.position.whiteStones.contains(p) -> whiteTerritory += p
                                                ownership < -0.6 && !state.position.blackStones.contains(p) -> blackTerritory += p
                                                ownership >= -0.6 && ownership < 0.6 -> removedSpots += p  // dame
                                            }
                                        }
                                    }
                                    val newPos = state.position.copy(
                                        blackTerritory = blackTerritory,
                                        whiteTerritory = whiteTerritory,
                                        removedSpots = removedSpots,
                                    )

                                    val whiteScore = (newPos.komi ?: 0f) + newPos.whiteTerritory.size + newPos.whiteCaptureCount + newPos.blackDeadStones.size
                                    val blackScore = newPos.blackTerritory.size + newPos.blackCaptureCount + newPos.whiteDeadStones.size
                                    val whiteWon = blackScore < whiteScore
                                    ScoreComputed(newPos, whiteScore, blackScore, whiteWon, it)
                                }
                                .subscribeOn(Schedulers.io())
                                .toObservable()
                                .doOnError(this::onError)
                                .onErrorResumeNext(Observable.empty())
                    }

    private fun toggleAI(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
        actions.filter { it is ToggleAIBlack || it is ToggleAIWhite }
            .withLatestFrom(state)
            .filter{ (sideChanged, state) -> state.engineStarted && state.position != null
                    && state.position?.nextToMove == when(sideChanged) {
                ToggleAIBlack -> StoneType.BLACK
                ToggleAIWhite -> StoneType.WHITE
                else -> null
            } }
            .map { NextPlayerChanged }

    private fun onError(throwable: Throwable) {
        Log.e("GameTurnMiddleware", throwable.message, throwable)
        recordException(throwable)
    }
}
