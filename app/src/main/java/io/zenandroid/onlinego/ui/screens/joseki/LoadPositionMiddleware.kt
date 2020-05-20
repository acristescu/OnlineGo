package io.zenandroid.onlinego.ui.screens.joseki

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.data.repositories.JosekiRepository
import org.koin.core.context.KoinContextHandler.get
import org.koin.java.KoinJavaComponent.inject

class LoadPositionMiddleware(
        private val josekiRepository: JosekiRepository
): Middleware<JosekiExplorerState, JosekiExplorerAction> {
    override fun bind(
            actions: Observable<JosekiExplorerAction>,
            state: Observable<JosekiExplorerState>
    ): Observable<JosekiExplorerAction> {

        return actions.ofType(LoadPosition::class.java)
                .switchMap {
                    josekiRepository.getJosekiPosition(it.id)
                            .subscribeOn(Schedulers.io())
                            .map<JosekiExplorerAction>(::PositionLoaded)
                            .onErrorReturn(::DataLoadingError)
                            .toObservable()
                            .startWith(StartDataLoading(it.id))
                }
    }
}