package io.zenandroid.onlinego.ui.screens.joseki

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.repositories.JosekiRepository
import io.zenandroid.onlinego.gamelogic.RulesManager.coordinateToCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JosekiExplorerViewModel(
    private val josekiRepository: JosekiRepository
) : ViewModel() {
    
    private val disposables = CompositeDisposable()
    private val analytics = OnlineGoApplication.instance.analytics
    
    private val _state = MutableStateFlow(JosekiExplorerState())
    val state: StateFlow<JosekiExplorerState> = _state.asStateFlow()
    
    private var lastRequestedNodeId: Long? = null

    init {
        loadPosition(null)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun onTappedCoordinate(coordinate: Cell) {
        analytics.logEvent("joseki_tapped_coordinate", null)
        
        val currentState = _state.value
        if (currentState.loading) return

        currentState.position?.next_moves?.find {
            it.placement != null && it.placement != "pass" && coordinateToCell(it.placement!!) == coordinate
        }?.let {
            loadPosition(it.node_id)
        } ?: run {
            showCandidateMove(null)
        }
    }

    fun onHotTrackedCoordinate(coordinate: Cell) {
        val currentState = _state.value
        if (!currentState.loading) {
            showCandidateMove(coordinate)
        }
    }

    fun onPressedPrevious() {
        analytics.logEvent("joseki_previous", null)

        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _state.value
            if (currentState.historyStack.isEmpty()) {
                finishExplorer()
            } else {
                val history = currentState.historyStack.dropLast(1)
                val position = currentState.historyStack.last()
                val nextPosStack = currentState.nextPosStack + currentState.position!!

                val boardPosition = Position.fromJosekiPosition(position)

                _state.value = currentState.copy(
                    position = position,
                    description = descriptionOfPosition(position),
                    boardPosition = boardPosition,
                    historyStack = history,
                    nextPosStack = nextPosStack,
                    loading = false,
                    candidateMove = null,
                    error = null,
                    previousButtonEnabled = history.isNotEmpty(),
                    nextButtonEnabled = true,
                    passButtonEnabled = position.next_moves?.find { it.placement == "pass" } != null
                )
            }
        }
    }

    fun onPressedNext() {
        viewModelScope.launch(Dispatchers.Default) {
            analytics.logEvent("joseki_next", null)

            val currentState = _state.value
            if (currentState.nextPosStack.isEmpty()) return@launch

            val nextPosStack = currentState.nextPosStack.dropLast(1)
            val position = currentState.nextPosStack.last()
            val history = currentState.historyStack + currentState.position!!

            val boardPosition = Position.fromJosekiPosition(position)

            _state.value = currentState.copy(
                position = position,
                description = descriptionOfPosition(position),
                boardPosition = boardPosition,
                historyStack = history,
                nextPosStack = nextPosStack,
                loading = false,
                candidateMove = null,
                error = null,
                previousButtonEnabled = true,
                nextButtonEnabled = nextPosStack.isNotEmpty(),
                passButtonEnabled = position.next_moves?.find { it.placement == "pass" } != null
            )
        }
    }

    fun onPressedPass() {
        analytics.logEvent("joseki_tenuki", null)
        
        val currentState = _state.value
        val passMove = currentState.position?.next_moves?.find { it.placement == "pass" }
        if (passMove != null) {
            loadPosition(passMove.node_id)
        }
    }

    fun loadPosition(id: Long?) {
        analytics.logEvent("joseki_load_position", null)
        
        val currentState = _state.value
        lastRequestedNodeId = id
        
        _state.value = currentState.copy(
            loading = true,
            lastRequestedNodeId = id,
            previousButtonEnabled = false
        )

        disposables.clear()
        val disposable = josekiRepository.getJosekiPosition(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { position -> onPositionLoaded(position) },
                { error -> onDataLoadingError(error) }
            )
        
        disposables.add(disposable)
    }

    private fun onPositionLoaded(position: JosekiPosition) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _state.value

            if (currentState.lastRequestedNodeId == null || currentState.lastRequestedNodeId == position.node_id) {
                val history =
                    if (currentState.position != null && currentState.position.node_id != position.node_id) {
                        currentState.historyStack + currentState.position
                    } else {
                        currentState.historyStack
                    }

                val boardPosition = Position.fromJosekiPosition(position)

                _state.value = currentState.copy(
                    position = position,
                    description = descriptionOfPosition(position),
                    boardPosition = boardPosition,
                    historyStack = history,
                    nextPosStack = emptyList(),
                    loading = false,
                    candidateMove = null,
                    error = null,
                    previousButtonEnabled = history.isNotEmpty(),
                    nextButtonEnabled = false,
                    passButtonEnabled = position.next_moves?.find { it.placement == "pass" } != null
                )
            }
        }
    }

    private fun onDataLoadingError(error: Throwable) {
        analytics.logEvent("joseki_loading_error", Bundle().apply {
            putString("ERROR_DETAILS", error.message)
            putString("ERROR_STATE", _state.value.toString())
        })

        _state.update { currentState ->
            currentState.copy(
                loading = false,
                error = error,
            )
        }
    }

    private fun showCandidateMove(placement: Cell?) {
        _state.update {
            it.copy(candidateMove = placement)
        }
    }

    private fun finishExplorer() {
        analytics.logEvent("joseki_finish", null)

        _state.update {
            it.copy(shouldFinish = true)
        }
    }

    private fun descriptionOfPosition(pos: JosekiPosition?): String? {
        return if (pos == null || pos.placement == "root") {
            "## Joseki Explorer\n" +
                    "*Joseki* is an English loanword from Japanese, usually referring to " +
                    "standard sequences of moves played out in a corner that result in a locally even exchange.\n" +
                    "### How to use the Joseki Explorer\n" +
                    "The marked moves below represent interesting continuations to the current position. " +
                    "The colours represent how good the move is considered to be: \n" +
                    "* Green moves are considered optimal\n" +
                    "* Yellow moves are considered OK\n" +
                    "* Red moves are considered mistakes\n" +
                    "* Purple markers are for trick plays\n" +
                    "* Blue for open questions.\n" +
                    "You can tap any of these interesting moves to explore the positions they lead to.\n" +
                    "### Tenuki\n" +
                    "Sometimes the best move is to play somewhere else. This is normally referred to as " +
                    "*tenuki*. If tenuki is considered an interesting option for the current position the " +
                    "pass button in the bottom left is enabled and you can press it to see what positions may " +
                    "arise after the current player tenukis."
        } else {
            pos.description
        }
    }
}