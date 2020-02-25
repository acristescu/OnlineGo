package io.zenandroid.onlinego.joseki

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.mvi.Middleware

class HotTrackMiddleware: Middleware<JosekiExplorerState, JosekiExplorerAction> {
    override fun bind(actions: Observable<JosekiExplorerAction>, state: Observable<JosekiExplorerState>): Observable<JosekiExplorerAction> {
        return actions.ofType(JosekiExplorerAction.UserHotTrackedCoordinate::class.java)
                .withLatestFrom(state)
                .filter { (_, state) -> !state.loading }
                .map { (action, _) -> JosekiExplorerAction.ShowCandidateMove(action.coordinate) }
    }
}