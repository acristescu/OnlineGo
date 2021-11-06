package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.graphics.Point
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class GameTurnMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            Observable.merge(
                    engineStarted(actions, state),
                    newGame(actions),
                    nextMove(actions, state),
                    computeScore(actions, state)
            )

    private fun engineStarted(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.ofType(EngineStarted::class.java)
                    .withLatestFrom(state)
                    .filter { (_, state) -> state.position != null && !(state.position.isGameOver() && state.aiWon != null) }
                    .map { (_, state) -> NewPosition(state.position!!) }

    private fun newGame(actions: Observable<AiGameAction>) =
            actions.ofType(NewGame::class.java)
                    .map { NewPosition(RulesManager.initializePosition(it.size, it.handicap)) }

    private fun nextMove(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.filter { it is NewPosition || it is AIMove }
                    .withLatestFrom(state)
                    .filter { (_, state) -> state.position?.isGameOver() == false }
                    .map { (_, state) ->
                        val isBlacksTurn = state.position?.nextToMove != StoneType.WHITE
                        if (isBlacksTurn == state.enginePlaysBlack) {
                            GenerateAiMove
                        } else {
                            PromptUserForMove
                        }
                    }

    private fun computeScore(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.filter { it is NewPosition || it is AIMove }
                    .withLatestFrom(state)
                    .filter { (_, state) -> state.position?.isGameOver() == true }
                    .flatMapSingle { (_, state) ->
                        KataGoAnalysisEngine.analyzePosition(
                                pos = state.position!!,
                                maxVisits = 10,
                                komi = state.position.komi,
                                includeOwnership = true
                        )
                                .map {
                                    val newPos = state.position.clone().apply {
                                        aiAnalysisResult = it
                                        clearAllMarkedTerritory()
                                        for(i in 0 until boardWidth) {
                                            for(j in 0 until boardHeight) {
                                                val p = Point(i, j)
                                                val ownership = it.ownership!![j*boardWidth + i] // a float between -1 and 1, -1 is 100% solid black territory, 1 is 100% solid white territory
                                                when(getStoneAt(p)) {
                                                    StoneType.WHITE -> {
                                                        if(ownership < 0) {
                                                            markBlackTerritory(p)
                                                            markRemoved(p)
                                                        }
                                                    }
                                                    StoneType.BLACK -> {
                                                        if(ownership > 0) {
                                                            markWhiteTerritory(p)
                                                            markRemoved(p)
                                                        }
                                                    }
                                                    null -> when {
                                                        ownership > 0.6 -> markWhiteTerritory(p)
                                                        ownership < -0.6 -> markBlackTerritory(p)
                                                        else -> markRemoved(p)  // dame
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val whiteScore = (newPos.komi ?: 0f) + newPos.whiteTerritory.size + newPos.whiteCapturedCount + newPos.blackDeadStones.size
                                    val blackScore = newPos.blackTerritory.size + newPos.blackCapturedCount + newPos.whiteDeadStones.size
                                    val aiWon = state.enginePlaysBlack == (blackScore > whiteScore)
                                    ScoreComputed(newPos, whiteScore, blackScore, aiWon)
                                }
                                .subscribeOn(Schedulers.io())
                    }
}