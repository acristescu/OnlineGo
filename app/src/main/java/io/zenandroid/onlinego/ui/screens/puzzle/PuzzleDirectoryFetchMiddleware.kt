package io.zenandroid.onlinego.ui.screens.puzzle

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectoryAction.*
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import org.koin.core.context.GlobalContext.get
import org.koin.java.KoinJavaComponent.inject

class PuzzleDirectoryFetchMiddleware(
        private val puzzleRepository: PuzzleRepository
): Middleware<PuzzleDirectoryState, PuzzleDirectoryAction> {
    override fun bind(
            actions: Observable<PuzzleDirectoryAction>,
            state: Observable<PuzzleDirectoryState>
    ): Observable<PuzzleDirectoryAction> {

        return actions.ofType(LoadPuzzle::class.java)
                .switchMap {
                    puzzleRepository.getPuzzle(it.id)
                            .subscribeOn(Schedulers.io())
                            .map<PuzzleDirectoryAction>(::PuzzleLoaded)
                            .onErrorReturn(::DataLoadingError)
                            .toObservable()
                            .startWith(WaitPuzzle(it.id))
                }
    }
}
