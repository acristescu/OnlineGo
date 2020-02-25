package io.zenandroid.onlinego.joseki

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.mvi.Middleware

class TriggerLoadingMiddleware : Middleware<JosekiExplorerState, JosekiExplorerAction> {
    override fun bind(actions: Observable<JosekiExplorerAction>, state: Observable<JosekiExplorerState>): Observable<JosekiExplorerAction> {
        val initialLoadingObservable = actions
                .ofType(ViewReady::class.java)
                .withLatestFrom(state)
                .filter { (_, state) -> state.position == null }
                .map <JosekiExplorerAction> { LoadPosition(null) }
        val subsequentLoadingObservable = actions
                .ofType(UserTappedCoordinate::class.java)
                .withLatestFrom(state)
                .flatMap <JosekiExplorerAction> { (action, state) ->
                    state?.position?.next_moves?.find {
                        it.placement != null && it.placement != "pass" && Position.coordinateToPoint(it.placement) == action.coordinate
                    }?.let {
                        Observable.just(LoadPosition(it.node_id))
                    } ?: Observable.just(ShowCandidateMove(null))
                }

        return initialLoadingObservable.mergeWith(subsequentLoadingObservable)
    }
}