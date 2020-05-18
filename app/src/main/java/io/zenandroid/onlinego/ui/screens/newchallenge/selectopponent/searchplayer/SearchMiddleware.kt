package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.data.repositories.PlayersRepository

class SearchMiddleware : Middleware<SearchPlayerState, SearchPlayerAction> {

    override fun bind(
        actions: Observable<SearchPlayerAction>,
        state: Observable<SearchPlayerState>
    ): Observable<SearchPlayerAction> =
        actions.ofType(SearchPlayerAction.Search::class.java)
                .filter { it.query.isNotBlank() }
                .flatMap { action ->
                    PlayersRepository.searchPlayers(action.query)
                            .subscribeOn(Schedulers.io())
                            .map<SearchPlayerAction> { SearchPlayerAction.Results(action.query, it) }
                            .toObservable()
                            .startWith( SearchPlayerAction.SearchStarted )
                }
}