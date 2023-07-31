package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer

import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.utils.recordException

class SearchMiddleware(
        private val playersRepository: PlayersRepository
) : Middleware<SearchPlayerState, SearchPlayerAction> {

    override fun bind(
            actions: Observable<SearchPlayerAction>,
            state: Observable<SearchPlayerState>
    ): Observable<SearchPlayerAction> =
            actions.ofType(SearchPlayerAction.Search::class.java)
                    .filter { it.query.isNotBlank() }
                    .flatMap { action ->
                        playersRepository.searchPlayers(action.query)
                                .subscribeOn(Schedulers.io())
                                .map<SearchPlayerAction> { SearchPlayerAction.Results(action.query, it) }
                                .toObservable()
                                .startWith(SearchPlayerAction.SearchStarted)
                                .doOnError(this::onError)
                                .onErrorResumeNext(Observable.empty())
                    }

    private fun onError(throwable: Throwable) {
        Log.e("SearchMiddleware", throwable.message, throwable)
        recordException(throwable)
    }
}