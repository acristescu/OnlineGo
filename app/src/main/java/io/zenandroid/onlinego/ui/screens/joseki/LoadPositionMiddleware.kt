package io.zenandroid.onlinego.ui.screens.joseki

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.data.repositories.JosekiRepository

class LoadPositionMiddleware: Middleware<JosekiExplorerState, JosekiExplorerAction> {
    override fun bind(
            actions: Observable<JosekiExplorerAction>,
            state: Observable<JosekiExplorerState>
    ): Observable<JosekiExplorerAction> {

        return actions.ofType(LoadPosition::class.java)
                .switchMap {
                    JosekiRepository.getJosekiPosition(it.id)
                            .subscribeOn(Schedulers.io())
                            .map<JosekiExplorerAction>(::PositionLoaded)
                            .onErrorReturn(::DataLoadingError)
                            .toObservable()
                            .startWith(StartDataLoading(it.id))
                }
    }
}