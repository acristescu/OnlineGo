package io.zenandroid.onlinego.ui.screens.puzzle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.Executors

class PuzzleSetViewModel (
    private val puzzleRepository: PuzzleRepository,
    private val restService: OGSRestService,
    private val settingsRepository: SettingsRepository,
    private val collectionId: Long
): ViewModel() {
    private val _state = MutableStateFlow(PuzzleSetState(boardTheme = settingsRepository.boardTheme))
    val state: StateFlow<PuzzleSetState> = _state

    private val workerPool = Executors.newCachedThreadPool()
    private val workerThread = newSingleThreadContext("PSVM")

    init {
        puzzleRepository.getPuzzleCollection(collectionId)
            .flowOn(Dispatchers.IO)
            .onEach { setCollection(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)

        puzzleRepository.getPuzzleCollectionContents(collectionId)
            .flowOn(Dispatchers.IO)
            .onEach { setCollectionPuzzles(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)

        puzzleRepository.markPuzzleCollectionVisited(collectionId)
            .flowOn(Dispatchers.IO)
            .catch { onError(it) }
            .launchIn(viewModelScope + workerThread)
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

    fun fetchSolutions(puzzleId: Long): Job {
        return puzzleRepository.getPuzzleSolution(puzzleId)
            .flowOn(workerPool.asCoroutineDispatcher())
            .onEach { updateSolutions(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)
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

    private fun fetchPuzzle(puzzleId: Long): Job {
        return puzzleRepository.getPuzzle(puzzleId)
            .flowOn(Dispatchers.IO)
            .onEach { setPuzzle(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)
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

val Puzzle.position: Position?
    get() {
        this.puzzle.let {
            return RulesManager.buildPos(
                moves = emptyList(),
                boardWidth = it.width,
                boardHeight = it.height,
                whiteInitialState = it.initial_state.white.toCoordinateSet(),
                blackInitialState = it.initial_state.black.toCoordinateSet()
            )
        }
    }
