package io.zenandroid.onlinego.ui.screens.puzzle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PuzzleSetViewModel(
  private val puzzleRepository: PuzzleRepository,
  private val settingsRepository: SettingsRepository,
  private val collectionId: Long
) : ViewModel() {
  private val _state = MutableStateFlow(PuzzleSetState(boardTheme = settingsRepository.boardTheme))
  val state: StateFlow<PuzzleSetState> = _state

  private val errorHandler = CoroutineExceptionHandler { _, throwable -> onError(throwable) }

  init {
    viewModelScope.launch(errorHandler) { puzzleRepository.fetchPuzzleCollection(collectionId) }
    viewModelScope.launch(errorHandler) {
      puzzleRepository.observePuzzleCollection(collectionId)
        .catch { onError(it) }
        .collect { setCollection(it) }
    }

    viewModelScope.launch(errorHandler) {
      puzzleRepository.observePuzzleCollectionContents(collectionId)
        .catch { onError(it) }
        .collect { setCollectionPuzzles(it) }
    }

    viewModelScope.launch(errorHandler) {
      puzzleRepository.markPuzzleCollectionVisited(collectionId)
    }
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
    viewModelScope.launch(errorHandler) {
        puzzleRepository.fetchPuzzleSolutions(puzzleId)
    }
    viewModelScope.launch(errorHandler) {
      puzzleRepository.observePuzzleSolutions(puzzleId)
        .catch { onError(it) }
        .collect { updateSolutions(it) }
    }
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

  private fun onError(t: Throwable) {
    Log.e(this::class.java.canonicalName, t.message, t)
    recordException(t)
  }
}

// TODO: This is called in the composable and is potentially heavy. This needs to be handled in the
// ViewModel.
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
