package io.zenandroid.onlinego.ui.screens.puzzle

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MILLIS
import org.jsoup.Jsoup

class TsumegoViewModel(
    private val puzzleRepository: PuzzleRepository,
    private val settingsRepository: SettingsRepository,
    private val collectionId: Long,
    private val puzzleId: Long,
): ViewModel() {
    private val _state = MutableStateFlow(TsumegoState(
        boardTheme = settingsRepository.boardTheme,
        drawCoordinates = settingsRepository.showCoordinates,
    ))
    val state: StateFlow<TsumegoState> = _state
    var collectionContents by mutableStateOf(emptyList<Puzzle>())
        private set
    var collectionPositions by mutableStateOf(emptyMap<Long, Position>())
        private set
    var collectionRatings by mutableStateOf(emptyMap<Long, Float?>())
        private set
    private var cursor by mutableIntStateOf(0)

    private var moveReplyJob: Job? = null
    private var ratingsWatchJob: Job? = null
    private val errorHandler = CoroutineExceptionHandler { _, throwable -> onError(throwable) }

    init {
        viewModelScope.launch(errorHandler) { puzzleRepository.fetchPuzzle(puzzleId) }
        viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzle(puzzleId)
                .filterNotNull()
                .catch { onError(it) }
                .collect {
                    cursor = collectionContents.indexOfFirst { it.id == _state.value.puzzle?.id }
                    setPuzzle(it)
                }
        }

        viewModelScope.launch(errorHandler) {
            puzzleRepository.markPuzzleCollectionVisited(collectionId)
        }

        viewModelScope.launch(errorHandler) { puzzleRepository.fetchPuzzleCollection(collectionId) }
        viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzleCollection(collectionId)
                .filterNotNull()
                .catch { onError(it) }
                .collect(::setCollection)
        }
        viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzleCollectionContents(collectionId)
                .filterNotNull()
                .catch { onError(it) }
                .collect(::setPuzzleCollectionContents)
        }

        ratingsWatchJob = viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzleRating(puzzleId)
                .filterNotNull()
                .cancellable()
                .catch { updateRating(-1) }
                .collect { updateRating(it.rating) }
        }
    }

    private fun setPuzzle(puzzle: Puzzle, attempts: Int = 1) {
        _state.update {
            it.copy(
                puzzle = puzzle,
                description = parseHtml(puzzle.puzzle.puzzle_description),
                boardPosition = puzzle.puzzle.let {
                    RulesManager.buildPos(moves = emptyList(),
                        boardWidth = it.width, boardHeight = it.height,
                        whiteInitialState = it.initial_state.white.toCoordinateSet(),
                        blackInitialState = it.initial_state.black.toCoordinateSet(),
                        marks = it.move_tree.marks.orEmpty().map { markData ->
                            val cell = Cell(x = markData.x, y = markData.y)
                            Mark(cell, markData.marks.toString(), PlayCategory.LABEL)
                        }.toSet(),
                        nextToMove = when(puzzle.puzzle.initial_player) {
                            "white" -> StoneType.WHITE
                            "black" -> StoneType.BLACK
                            else -> StoneType.BLACK
                        }
                    )
                },
                attemptCount = attempts,
                sgfMoves = "",
                continueButtonVisible = false,
                retryButtonVisible = false,
                nodeStack = ArrayDeque(listOf(puzzle.puzzle.move_tree))
            )
        }

        fetchSolutions()
    }

    private fun setPuzzleCollectionContents(puzzles: List<Puzzle>) {
        collectionContents = puzzles
        cursor = collectionContents.indexOfFirst { it.id == _state.value.puzzle?.id }
    }

    fun renderCollectionPuzzle(index: Int) {
        val puzzle = collectionContents[index]

        viewModelScope.launch(errorHandler) {
            withContext(Dispatchers.IO) {
                val position = puzzle.puzzle.let {
                    RulesManager.buildPos(
                        moves = emptyList(),
                        boardWidth = it.width,
                        boardHeight = it.height,
                        whiteInitialState = it.initial_state.white.toCoordinateSet(),
                        blackInitialState = it.initial_state.black.toCoordinateSet()
                    )
                } ?: return@withContext

                withContext(Dispatchers.Main) {
                    collectionPositions = collectionPositions.toMutableMap().apply {
                        put(puzzle.id, position)
                    }
                }
            }
        }

        viewModelScope.launch(errorHandler) {
            val ratingFlow = puzzleRepository.observePuzzleRating(puzzle.id)
                .filterNotNull()
                .map { it.rating.toFloat() }
                .catch { emit(0f) }

            val solutionsFlow = puzzleRepository.observePuzzleSolutions(puzzle.id)
                .catch { emit(emptyList()) }

            combine(ratingFlow, solutionsFlow) { rating, solutions ->
                if (solutions.isEmpty()) null
                else rating
            }.collect {
                collectionRatings = collectionRatings.toMutableMap().apply {
                    put(puzzle.id, it)
                }
            }
        }
    }

    val hasNextPuzzle: Boolean by derivedStateOf {
        cursor < collectionContents.size - 1
    }

    val hasPreviousPuzzle: Boolean by derivedStateOf {
        cursor > 0
    }

    fun nextPuzzle() {
        if(!hasNextPuzzle) return

        val index = cursor + 1
        cursor = index
        val puzzle = collectionContents[index]
        setPuzzle(puzzle)
        fetchRating()
    }

    fun resetPuzzle() {
        val puzzle = _state.value.puzzle!!
        val attempts = _state.value.attemptCount + 1
        setPuzzle(puzzle, attempts)
    }

    fun selectPuzzle(index: Int) {
        if(index < 0 || index >= collectionContents.size) return

        cursor = index
        val puzzle = collectionContents[index]
        setPuzzle(puzzle)
        fetchRating()
    }

    fun previousPuzzle() {
        if(!hasPreviousPuzzle) return

        val index = cursor - 1
        cursor = index
        val puzzle = collectionContents[index]
        setPuzzle(puzzle)
        fetchRating()
    }

    fun makeMove(move: Cell) {
        _state.value.nodeStack.let { stack ->
            val branches = stack.lastOrNull()?.branches ?: emptyList()
            val branch = branches.find {
                it.x == move.x && it.y == move.y
            } ?: branches.find { it.x == -1 || it.y == -1 }
            branch?.let { node ->
                moveReplyJob = viewModelScope.launch(errorHandler) {
                    var position = state.value.boardPosition!!
                    position = (RulesManager.makeMove(position, position.nextToMove, move)
                        ?: run {
                            _state.value = state.value.copy(
                                hoveredCell = null
                            )
                            return@launch
                        }).copy(nextToMove = position.nextToMove.opponent)
                    val nodeStack = _state.value.nodeStack
                    nodeStack.addLast(node)
                    var moveString = _state.value.sgfMoves
                    moveString += Util.getSGFCoordinates(move)
                    node.branches?.randomOrNull()?.let { moveTree ->
                        val reply = Cell(moveTree.x, moveTree.y)
                        nodeStack.addLast(moveTree)
                        _state.update {
                            it.copy(
                                boardPosition = position.let { pos ->
                                    pos.copy(customMarks = pos.customMarks.plus(node.marks.orEmpty().map { markData ->
                                        val cell = Cell(x = markData.x, y = markData.y)
                                        Mark(cell, markData.marks.toString(), PlayCategory.LABEL)
                                    }))
                                },
                                nodeStack = nodeStack,
                                sgfMoves = moveString,
                                continueButtonVisible = if(moveTree.correct_answer == true) true
                                else _state.value.continueButtonVisible,
                            )
                        }
                        delay(600)
                        moveString += Util.getSGFCoordinates(reply)
                        position = (RulesManager.makeMove(position, position.nextToMove, reply)
                            ?: throw RuntimeException("Invalid move $moveTree")).copy(nextToMove = position.nextToMove.opponent)
                    }
                    _state.update {
                        it.copy(
                            boardPosition = position.let { pos ->
                                pos.copy(customMarks = pos.customMarks.plus(node.marks.orEmpty().map { markData ->
                                    val cell = Cell(x = markData.x, y = markData.y)
                                    Mark(cell, markData.marks.toString(), PlayCategory.LABEL)
                                }))
                            },
                            nodeStack = nodeStack,
                            sgfMoves = moveString,
                            continueButtonVisible = if(node.correct_answer == true) true
                            else _state.value.continueButtonVisible,
                            retryButtonVisible = true,
                            hoveredCell = null,
                        )
                    }
                }
            } ?: run launch@{
                var position = state.value.boardPosition!!
                position = (RulesManager.makeMove(position, position.nextToMove, move)
                    ?: run {
                        _state.update {
                            it.copy(hoveredCell = null)
                        }
                        return@launch
                    }).copy(nextToMove = position.nextToMove.opponent)
                val nodeStack = _state.value.nodeStack
                nodeStack.addLast(null)
                _state.update {
                    it.copy(
                        boardPosition = position,
                        nodeStack = nodeStack,
                        retryButtonVisible = true,
                        hoveredCell = null,
                    )
                }
            }
        }
        if (_state.value.continueButtonVisible) {
            markSolved()
        }
    }

    fun addBoardHints() {
        _state.update {
            it.copy(boardInteractive = false)
        }
        fun isHappyPath(node: MoveTree): Boolean
            = node.correct_answer == true || node.branches?.any { isHappyPath(it) } == true
        val moves = _state.value.nodeStack.last()?.branches?.map {
            if(isHappyPath(it)) {
                if(it.correct_answer == true) {
                    Mark(Cell(it.x, it.y), "️", PlayCategory.IDEAL)
                } else {
                    Mark(Cell(it.x, it.y), "️", PlayCategory.GOOD)
                }
            } else {
                Mark(Cell(it.x, it.y), "", PlayCategory.MISTAKE)
            }
        } ?: emptyList()
        val position = _state.value.boardPosition!!.let {
            it.copy(customMarks = it.customMarks.plus(moves))
        }
        _state.update {
            it.copy(
                boardPosition = position,
                boardInteractive = true
            )
        }
    }

    private fun setCollection(collection: PuzzleCollection) {
        _state.update {
            it.copy(collection = collection)
        }
    }

    private fun markSolved() {
        viewModelScope.launch(errorHandler) {
            val record = _state.value.let { PuzzleSolution(
                puzzle = it.puzzle!!.id,
                time_elapsed = it.startTime?.let { MILLIS.between(it, now()) } ?: 0,
                attempts = it.attemptCount,
                solution = it.sgfMoves,
            ) }

            puzzleRepository.markPuzzleSolved(_state.value.puzzle?.id!!, record)
        }
    }

    private fun fetchSolutions() {
        _state.update { it.copy(solutions = emptyList()) }
        viewModelScope.launch(errorHandler) { puzzleRepository.fetchPuzzleSolutions(_state.value.puzzle?.id!!) }
        viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzleSolutions(_state.value.puzzle?.id!!)
                .collect(::updateSolutions)
        }
    }

    private fun updateSolutions(solution: List<PuzzleSolution>) {
        _state.update {
            it.copy(solutions = solution)
        }
    }

    fun rate(value: Int) {
        viewModelScope.launch(errorHandler) {
            puzzleRepository.ratePuzzle(_state.value.puzzle?.id!!, value)
                .catch { onError(it) }
                .collect { updateRating(value) }
        }
    }

    private fun fetchRating(id: Long? = null) {
        viewModelScope.launch(errorHandler) {
            puzzleRepository.fetchPuzzleRating(id ?: _state.value.puzzle?.id!!)
        }
        ratingsWatchJob?.cancel()
        ratingsWatchJob = viewModelScope.launch(errorHandler) {
            puzzleRepository.observePuzzleRating(id ?: _state.value.puzzle?.id!!)
                .filterNotNull()
                .cancellable()
                .catch { updateRating(-1) }
                .collect { updateRating(it.rating) }
        }
    }

    private fun updateRating(value: Int) {
        _state.update {
            it.copy(
                rating = PuzzleRating(rating = value)
            )
        }
    }

    private fun parseHtml(body: String): String {
        val document = Jsoup.parseBodyFragment(body)
        return document.body().text()
    }

    private fun onError(t: Throwable) {
        Log.e(this::class.java.canonicalName, t.message, t)
        recordException(t)
    }
}
