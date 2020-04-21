package io.zenandroid.onlinego.joseki

import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.mvi.Middleware

class AnalyticsMiddleware: Middleware<JosekiExplorerState, JosekiExplorerAction> {
    private var analytics = OnlineGoApplication.instance.analytics

    override fun bind(actions: Observable<JosekiExplorerAction>, state: Observable<JosekiExplorerState>): Observable<JosekiExplorerAction> {
        return actions.withLatestFrom(state)
                .doOnNext { (action, state) ->
                    when(action) {
                        is DataLoadingError -> analytics.logEvent("joseki_loading_error", Bundle().apply {
                            putString("ERROR_DETAILS", action.e.message)
                            putString("ERROR_STATE", state.toString())
                        })
                        Finish -> analytics.logEvent("joseki_finish", null)
                        is UserTappedCoordinate -> analytics.logEvent("joseki_tapped_coordinate", null)
                        is LoadPosition -> analytics.logEvent("joseki_load_position", null)
                        UserPressedPrevious -> analytics.logEvent("joseki_previous", null)
                        UserPressedBack -> analytics.logEvent("joseki_back", null)
                        UserPressedNext -> analytics.logEvent("joseki_next", null)
                        UserPressedPass -> analytics.logEvent("joseki_tenuki", null)

                        ViewReady, is PositionLoaded, is StartDataLoading, is ShowCandidateMove, is UserHotTrackedCoordinate -> {}
                    }
                }
                .switchMap { Observable.empty<JosekiExplorerAction>() }
    }

}