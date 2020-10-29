package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.graphics.Point
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class UserMoveMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        val source = Observable.merge(
                actions.ofType(AiGameAction.UserTappedCoordinate::class.java).map { it.coordinate },
                actions.ofType(AiGameAction.UserPressedPass::class.java).map { Point(-1, -1) }
        )

        return source
                .withLatestFrom(state)
                .filter { (_, state) -> state.position != null }
                .flatMap { (coordinate, state) ->
                    val newPos = RulesManager.makeMove(state.position!!, state.position.nextToMove, coordinate)
                    if (newPos != null) {
                        newPos.nextToMove = newPos.nextToMove.opponent
                        Observable.just(AiGameAction.NewPosition(newPos))
                    } else {
                        Observable.empty()
                    }
                }
    }
}