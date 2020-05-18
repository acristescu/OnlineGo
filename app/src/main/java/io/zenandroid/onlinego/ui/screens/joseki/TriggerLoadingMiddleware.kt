package io.zenandroid.onlinego.ui.screens.joseki

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.mvi.Middleware

class TriggerLoadingMiddleware : Middleware<JosekiExplorerState, JosekiExplorerAction> {
    override fun bind(actions: Observable<JosekiExplorerAction>, state: Observable<JosekiExplorerState>): Observable<JosekiExplorerAction> {
        val initialLoadingObservable = actions
                .ofType(ViewReady::class.java)
                .withLatestFrom(state)
                .filter { (_, state) -> state.position == null }
                .map <JosekiExplorerAction> { LoadPosition(null) }
        val coordinateLoadingObservable = actions
                .ofType(UserTappedCoordinate::class.java)
                .withLatestFrom(state)
                .filter { (_, state) -> !state.loading }
                .flatMap <JosekiExplorerAction> { (action, state) ->
                    state?.position?.next_moves?.find {
                        it.placement != null && it.placement != "pass" && Position.coordinateToPoint(it.placement!!) == action.coordinate
                    }?.let {
                        Observable.just(LoadPosition(it.node_id))
                    } ?: Observable.just(ShowCandidateMove(null))
                }

        val passLoadingObservable = actions
                .ofType(UserPressedPass::class.java)
                .withLatestFrom(state)
                .map { (_, state) ->
                    LoadPosition(state?.position?.next_moves?.find { it.placement == "pass" }?.node_id)
                }

        return Observable.merge(
                initialLoadingObservable,
                coordinateLoadingObservable,
                passLoadingObservable
        )
    }
}