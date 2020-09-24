package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.leela.LeelaZeroService
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class GameTurnMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
        actions.filter { it is EngineStarted  || it is NewPosition }
                .withLatestFrom(state)
                .map { (action, state) ->
                    if(action is EngineStarted) {
                        LeelaZeroService.clearBoard()
                        LeelaZeroService.setSecondsPerMove(state.secondsPerMove)
                        val startingPos = Position(state.boardSize).apply { nextToMove = if(state.handicap == 0) StoneType.BLACK else StoneType.WHITE }
                        if(state.handicap != 0) {
                            LeelaZeroService.setFixedHandicap(state.handicap, state.boardSize).forEach {
                                startingPos.putStone(it.x, it.y, StoneType.BLACK)
                            }
                        }
                        val pos = state.position ?: startingPos
                        LeelaZeroService.setPosition(pos)
                        NewPosition(pos)
                    } else {
                        val isBlacksTurn = state.position?.nextToMove != StoneType.WHITE
                        if (isBlacksTurn == state.leelaPlaysBlack) {
                            GenerateAiMove
                        } else {
                            PromptUserForMove
                        }
                    }
                }

}