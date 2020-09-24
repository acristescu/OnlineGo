package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.leela.LeelaZeroService
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class UndoRedoMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            Observable.merge(
                    actions.ofType(AiGameAction.UserPressedPrevious::class.java)
                            .withLatestFrom(state)
                            .doOnNext {
                                LeelaZeroService.undoMove()
                                LeelaZeroService.undoMove()
                            }
                            .switchMap { Observable.empty<AiGameAction>() },
                    actions.ofType(AiGameAction.UserPressedNext::class.java)
                            .withLatestFrom(state)
                            .doOnNext { (_, state) ->
                                val redoPos2 = state.position
                                val redoPos1 = state.position?.parentPosition!!
                                LeelaZeroService.playMove(redoPos1.parentPosition?.nextToMove ?: StoneType.BLACK, redoPos1.lastMove!!, state.position.boardSize)
                                LeelaZeroService.playMove(redoPos1.nextToMove, redoPos2?.lastMove!!, state.position.boardSize)
                            }
                            .switchMap { Observable.empty<AiGameAction>() }
            )
}