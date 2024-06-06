package io.zenandroid.onlinego.ui.screens.localai.middlewares

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class UserMoveMiddleware : Middleware<AiGameState, AiGameAction> {
  override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
    val source = Observable.merge(
        actions.ofType(AiGameAction.UserTappedCoordinate::class.java).map { it.coordinate },
        actions.ofType(AiGameAction.UserPressedPass::class.java).map { Cell(-1, -1) }
    )

    return source
        .withLatestFrom(state)
        .filter { (_, state) -> state.position != null }
        .flatMap { (coordinate, state) ->
          val isBlacksTurn = state.position?.nextToMove != StoneType.WHITE
          if (isBlacksTurn == state.enginePlaysBlack) {
            FirebaseCrashlytics.getInstance().log("User tried to move when it's not their turn")
            return@flatMap Observable.empty()
          }

          val newPos = RulesManager.makeMove(state.position!!, state.position.nextToMove, coordinate)
          when {
            newPos == null && state.position.getStoneAt(coordinate) != null -> Observable.empty()
            newPos == null -> Observable.just(AiGameAction.UserTriedSuicidalMove(coordinate))
            coordinate.x != -1 && RulesManager.isIllegalKO(state.history, newPos) -> Observable.just(AiGameAction.UserTriedKoMove(coordinate))
            else -> {
              Observable.just(AiGameAction.NewPosition(newPos))
            }
          }
        }
  }
}