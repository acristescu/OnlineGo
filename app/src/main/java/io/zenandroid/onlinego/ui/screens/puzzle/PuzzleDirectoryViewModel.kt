package io.zenandroid.onlinego.ui.screens.puzzle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.CountSort
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.NameSort
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.RatingSort
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.ViewsSort
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PuzzleDirectoryViewModel(
  private val puzzleRepository: PuzzleRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(
    PuzzleDirectoryState(
      boardTheme = settingsRepository.boardTheme,
      availableSorts = listOf(
        RatingSort(false),
        ViewsSort(false),
        NameSort(false),
        CountSort(false),
      ),
      currentSort = RatingSort(false),
    )
  )
  val state: StateFlow<PuzzleDirectoryState> = _state
  private val filterText = MutableStateFlow("")
  private val sortField = MutableStateFlow<PuzzleDirectorySort>(RatingSort(false))
  private val alreadyOpened = MutableStateFlow(false)
  private val errorHandler = CoroutineExceptionHandler { _, throwable -> onError(throwable) }

  init {
    viewModelScope.launch(errorHandler) { puzzleRepository.fetchAllPuzzleCollections() }
    viewModelScope.launch(errorHandler) {
      combine(
        puzzleRepository.observeAllPuzzleCollections(),
        filterText.map { it.lowercase() },
        sortField,
        alreadyOpened,
        ::filterCollections
      )
        .catch { onError(it) }
        .collect { setCollections(it) }
    }

    puzzleRepository.getRecentPuzzleCollections()
      .map {
        it.associateBy(VisitedPuzzleCollection::collectionId).plus(state.value.recents)
      }
      .flowOn(Dispatchers.IO)
      .onEach { setRecentCollections(it) }
      .catch { onError(it) }
      .launchIn(viewModelScope)

    puzzleRepository.getPuzzleCollectionSolutions()
      .flowOn(Dispatchers.IO)
      .onEach { setCollectionSolutions(it) }
      .catch { onError(it) }
      .launchIn(viewModelScope)
  }

  private fun filterCollections(
    collections: List<PuzzleCollection>,
    filter: String, sort: PuzzleDirectorySort,
    alreadyOpened: Boolean,
  ): List<PuzzleCollection> {
    return collections
      .filter {
        (it.name.lowercase().contains(filter) || it.owner?.username?.lowercase()
          ?.contains(filter) == true) &&
            (!alreadyOpened || state.value.recents.containsKey(it.id))
      }
      .sortedWith(sort.comparator)
  }

  private fun setCollections(collections: List<PuzzleCollection>) {
    _state.update {
      it.copy(collections = collections)
    }
  }

  private fun setRecentCollections(recents: Map<Long, VisitedPuzzleCollection>) {
    _state.update {
      it.copy(
        recents = recents,
        recentsPages = recents.map { it.value }
          .distinctBy { it.collectionId }
          .sortedByDescending { it.timestamp }
          .chunked(3)
      )
    }
  }

  private fun setCollectionSolutions(solutions: Map<Long, Int>) {
    _state.update {
      it.copy(solutions = solutions)
    }
  }

  suspend fun getFirstUnsolvedForCollection(collection: PuzzleCollection): Long {
    return puzzleRepository.getPuzzleCollectionFirstUnsolved(collection.id)
        ?: collection.starting_puzzle.id
  }

  private fun onError(t: Throwable) {
    Log.e(this::class.java.canonicalName, t.message, t)
    recordException(t)
  }

  fun onToggleOnlyOpened() {
    alreadyOpened.value = !alreadyOpened.value
    _state.update {
      it.copy(onlyOpenend = !it.onlyOpenend)
    }
  }

  fun onSortChanged(sort: PuzzleDirectorySort) {
    sortField.value = sort
    _state.update {
      it.copy(currentSort = sort)
    }
  }

  fun onFilterChanged(filter: String) {
    filterText.value = filter
    _state.update {
      it.copy(filterString = filter)
    }
  }
}
