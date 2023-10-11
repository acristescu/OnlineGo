package io.zenandroid.onlinego.ui.screens.puzzle

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PuzzleDirectoryViewModel (
    private val puzzleRepository: PuzzleRepository,
    private val restService: OGSRestService,
): ViewModel() {
    private val _state = MutableStateFlow(PuzzleDirectoryState())
    val state: StateFlow<PuzzleDirectoryState> = _state
    private val subscriptions = CompositeDisposable()

    init {
        puzzleRepository.getAllPuzzleCollections()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::addCollections, this::onError)
            .addToDisposable(subscriptions)
        puzzleRepository.getRecentPuzzleCollections()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setRecentCollections, this::onError)
            .addToDisposable(subscriptions)
        puzzleRepository.getPuzzleCollectionSolutions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setCollectionSolutions, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun addCollections(nextCollections: List<PuzzleCollection>) {
        _state.update {
            it.copy(
                collections = it.collections.plus(nextCollections.associateBy(PuzzleCollection::id))
            )
        }
    }

    private fun setRecentCollections(recents: List<VisitedPuzzleCollection>) {
        _state.update {
            it.copy(
                recents = recents.associateBy(VisitedPuzzleCollection::timestamp).plus(it.recents)
            )
        }
    }

    private fun setCollectionSolutions(solutions: Map<Long, Int>) {
        _state.update {
            it.copy(solutions = solutions)
        }
    }

    private fun onError(t: Throwable) {
        Log.e(this::class.java.canonicalName, t.message, t)
        recordException(t)
    }
}
