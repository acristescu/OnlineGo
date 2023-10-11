package io.zenandroid.onlinego.ui.screens.puzzle

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleSetAction.*
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PuzzleSetViewModel (
    private val puzzleRepository: PuzzleRepository,
    private val restService: OGSRestService,
    private val collectionId: Long
): ViewModel() {
    private val _state = MutableStateFlow(PuzzleSetState())
    val state: StateFlow<PuzzleSetState> = _state
    private val subscriptions = CompositeDisposable()

    private val workerPool = Executors.newCachedThreadPool()

    init {
        puzzleRepository.getPuzzleCollection(collectionId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setCollection, this::onError)
            .addToDisposable(subscriptions)
        puzzleRepository.getPuzzleCollectionContents(collectionId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setCollectionPuzzles, this::onError)
            .addToDisposable(subscriptions)
        puzzleRepository.markPuzzleCollectionVisited(collectionId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .subscribe({ }, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun setCollection(response: PuzzleCollection) {
        _state.update {
            it.copy(collection = response)
        }
    }

    private fun setCollectionPuzzles(response: List<Puzzle>) {
        _state.update {
            it.copy(puzzles = response)
        }
    }

    fun fetchSolutions(puzzleId: Long) {
        puzzleRepository.getPuzzleSolution(puzzleId)
            .subscribeOn(Schedulers.from(workerPool))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateSolutions, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun updateSolutions(solution: List<PuzzleSolution>? = null) {
        solution?.firstOrNull()?.puzzle?.let { puzzleId ->
            _state.update {
                it.copy(solutions = it.solutions.toMutableMap().also { solutions ->
                    solutions[puzzleId] = solution.toSet()
                })
            }
        }
    }

    private fun fetchPuzzle(puzzleId: Long) {
        return puzzleRepository.getPuzzle(puzzleId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .toObservable()
            .subscribe(this::setPuzzle, this::onError)
            .addToDisposable(subscriptions)
    }

    private fun setPuzzle(puzzle: Puzzle) {
        _state.update {
            it.copy(puzzles = (it.puzzles.orEmpty() + puzzle).ifEmpty { null })
        }
    }

    private fun onError(t: Throwable) {
        Log.e(this::class.java.canonicalName, t.message, t)
        recordException(t)
    }
}
