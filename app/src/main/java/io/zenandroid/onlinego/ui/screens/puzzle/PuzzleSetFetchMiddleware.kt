package io.zenandroid.onlinego.ui.screens.puzzle

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleSetAction.*
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import org.koin.core.context.GlobalContext.get
import org.koin.java.KoinJavaComponent.inject

class PuzzleSetFetchMiddleware(
        private val puzzleRepository: PuzzleRepository
): Middleware<PuzzleSetState, PuzzleSetAction> {
    override fun bind(
            actions: Observable<PuzzleSetAction>,
            state: Observable<PuzzleSetState>
    ): Observable<PuzzleSetAction> {

        return actions.ofType(LoadPuzzle::class.java)
                .switchMap {
                    puzzleRepository.getPuzzle(it.id)
                            .subscribeOn(Schedulers.io())
                            .map<PuzzleSetAction>(::PuzzleLoaded)
                            .onErrorReturn(::DataLoadingError)
                            .toObservable()
                            .startWith(WaitPuzzle(it.id))
                }
    }
}
