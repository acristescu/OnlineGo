package io.zenandroid.onlinego.joseki

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ogs.JosekiRepository
import io.zenandroid.onlinego.ogs.OGSServiceImpl

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