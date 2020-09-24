package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.leela.LeelaZeroService
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class AIMoveMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
        actions.ofType(GenerateAiMove::class.java)
                .withLatestFrom(state)
//                .filter { (_, state) ->
//                    val isBlacksTurn = state.position.nextToMove == StoneType.BLACK
//                    isBlacksTurn == state.leelaPlaysBlack
//                }
                .flatMap { (_, state) ->
                    LeelaZeroService.genmove(if(state.leelaPlaysBlack) StoneType.BLACK else StoneType.WHITE)
                            .map {
                                if(it.startsWith("=")) {
                                    val move = Util.getCoordinatesFromGTP(it.substring(2), state.boardSize)
                                    val side = if(state.leelaPlaysBlack) StoneType.BLACK else StoneType.WHITE
                                    val newPos = RulesManager.makeMove(state.position!!, side, move)!!
                                    newPos.nextToMove = newPos.nextToMove.opponent
                                    NewPosition(newPos)
                                } else {
                                    AIAnalysisLine(it)
                                }
                            }
                            .subscribeOn(Schedulers.io())
                }
}