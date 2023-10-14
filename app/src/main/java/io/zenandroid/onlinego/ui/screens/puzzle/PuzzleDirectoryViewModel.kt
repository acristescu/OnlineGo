package io.zenandroid.onlinego.ui.screens.puzzle

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.*
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import java.time.Instant
import java.util.SortedMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.Dispatchers

class PuzzleDirectoryViewModel (
    private val puzzleRepository: PuzzleRepository,
    private val restService: OGSRestService,
    private val settingsRepository: SettingsRepository,
): ViewModel() {
    private val _state = MutableStateFlow(PuzzleDirectoryState(boardTheme = settingsRepository.boardTheme))
    val state: StateFlow<PuzzleDirectoryState> = _state
    var filterText = MutableStateFlow("")
    var sortField = MutableStateFlow<PuzzleDirectorySort>(RatingSort(false))

    init {
        puzzleRepository.getAllPuzzleCollections()
            .toObservable().asFlow()
            .map {
                state.value.collections.plus(it.associateBy(PuzzleCollection::id))
            }
            .let {
                combine(it, filterText.map { it.lowercase() }, sortField, ::filterCollections)
            }
            .flowOn(Dispatchers.IO)
            .onEach { setCollections(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)

        puzzleRepository.getRecentPuzzleCollections()
            .toObservable().asFlow()
            .map {
                it.associateBy(VisitedPuzzleCollection::collectionId).plus(state.value.recents)
            }
            .flowOn(Dispatchers.IO)
            .onEach { setRecentCollections(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)

        puzzleRepository.getPuzzleCollectionSolutions()
            .toObservable().asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { setCollectionSolutions(it) }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    private fun filterCollections(collections: Map<Long, PuzzleCollection>,
            filter: String, sort: PuzzleDirectorySort): SortedMap<Long, PuzzleCollection> {
        return collections
            .filter { it.value.name.lowercase().contains(filter)
                    || it.value.owner?.username?.lowercase()?.contains(filter) == true }
            .let {
                val compare = sort.comparator::compare
                it.toSortedMap({ a, b -> compare(it[a], it[b]) })
            }
    }

    private fun setCollections(collections: SortedMap<Long, PuzzleCollection>) {
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
                  //.map { it.plus(listOf(null, null)).take(3) }
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
