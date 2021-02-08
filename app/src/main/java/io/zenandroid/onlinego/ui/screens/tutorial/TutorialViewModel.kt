package io.zenandroid.onlinego.ui.screens.tutorial

import android.graphics.Point
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Page
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.data.model.local.TutorialStep.*
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.populateWithAreas
import io.zenandroid.onlinego.gamelogic.Util.populateWithMarks
import io.zenandroid.onlinego.gamelogic.Util.populateWithSGF
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TutorialViewModel(
        private val tutorialsRepository: TutorialsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())

    val state: StateFlow<TutorialState> = _state
    var moveReplyJob: Job? = null

    private fun initialState() = TutorialState()

    fun loadTutorial(tutorialName: String) {
        if(_state.value.tutorial != null) {
            // probably just recreating the view, the ViewModel is unimpressed
            return
        }
        val tutorial = tutorialsRepository.loadTutorial(tutorialName)!!
        val step = tutorial.steps[0]
        loadStep(tutorial, step)
    }

    private fun loadStep(tutorial: Tutorial, step: TutorialStep) {
        when(step) {
            is Interactive -> {
                _state.value = _state.value.copy(
                        tutorialGroups = tutorialsRepository.getTutorialGroups(),
                        tutorial = tutorial,
                        step = step,
                        position = Position(step.size).apply { populateWithSGF(step.init) },
                        removedStones = null,
                        text = step.text,
                        node = null,
                        page = null,
                        retryButtonVisible = true,
                        nextButtonVisible = false,
                        boardInteractive = step.branches.isNotEmpty()
                )
            }
            is Lesson -> {
                val initialPage = step.pages[0]
                loadPage(tutorial, step, initialPage)
            }
            is GameExample -> {
                loadGame(tutorial, step)
            }
        }
    }

    fun acceptAction(action: HandledByViewModel) {
        val state = _state.value
        when(action) {
            is BoardCellHovered -> _state.value = state.copy(
                    hoveredCell = if(_state.value.position?.getStoneAt(action.point) != null) null else action.point
            )
            is BoardCellTapped -> makeMove(state, action.point)
            RetryPressed -> {
                moveReplyJob?.cancel()
                loadStep(state.tutorial!!, state.step!!)
            }
            NextPressed -> {
                when(state.step) {
                    is Lesson -> {
                        val nextPageIndex = state.step.pages.indexOf(state.page) + 1
                        if(nextPageIndex >= state.step.pages.size) {
                            onStepDone()
                        } else {
                            loadPage(state.tutorial, state.step, state.step.pages[nextPageIndex])
                        }
                    }
                    is GameExample -> {
                        if(state.gameExamplePositionIndex >= state.gameExamplePositions.size - 1) {
                            onStepDone()
                        } else {
                            _state.value = state.copy(
                                    gameExamplePositionIndex = state.gameExamplePositionIndex + 1,
                                    position = state.gameExamplePositions[state.gameExamplePositionIndex + 1],
                                    removedStones = null
                            )
                        }
                    }
                    else -> {
                        onStepDone()
                    }
                }
            }
        }
    }

    private fun loadGame(tutorial: Tutorial, game: GameExample) {
        val positions = Util.sgfToPositionList(game.sgf, game.size)
        _state.value = state.value.copy(
                gameExamplePositions = positions,
                gameExamplePositionIndex = 0,
                position = positions[0],
                removedStones = null,
                text = game.text,
                step = game
        )
    }

    private fun loadPage(tutorial: Tutorial?, step: Lesson, page: Page) {
        val pos = Position(step.size).apply {
            populateWithSGF(page.position)
            page.marks?.let { populateWithMarks(page.marks) }
            page.areas?.let { populateWithAreas(page.areas) }
        }
        _state.value = _state.value.copy(
                tutorialGroups = tutorialsRepository.getTutorialGroups(),
                tutorial = tutorial,
                step = step,
                page = page,
                text = page.text,
                position = pos,
                removedStones = null,
                node = null,
                retryButtonVisible = false,
                nextButtonVisible = true,
                boardInteractive = false
        )
    }

    private fun onStepDone() {
        val state = _state.value
        state.tutorial?.let {
            val nextStepIndex = it.steps.indexOf(state.step) + 1
            if(nextStepIndex >= it.steps.size) {
                onTutorialDone()
            } else {
                loadStep(it, it.steps[nextStepIndex])
            }
        }
    }

    private fun onTutorialDone() {
        val state = _state.value
        state.tutorial?.let { tutorial ->
            tutorialsRepository.markTutorialCompleted(tutorial)
            state.tutorialGroups?.let { groups ->
                groups.find { it.tutorials.contains(tutorial) } ?. let { parent ->
                    val nextTutorial = if(parent.tutorials.last() == tutorial) {
                        // finished a tutorial group
                        if(groups.last() == parent) {
                            // finished all tutorials
                            onAllTutorialsDone()
                            return
                        } else {
                            val nextGroup = groups[groups.indexOf(parent) + 1]
                            nextGroup.tutorials[0]
                        }
                    } else {
                        parent.tutorials[parent.tutorials.indexOf(tutorial) + 1]
                    }
                    val step = nextTutorial.steps[0]
                    loadStep(nextTutorial, step)
                }
            }
        }
    }

    private fun onAllTutorialsDone() {

    }

    private fun makeMove(state: TutorialState, move: Point) {
        (state.step as? Interactive)?.let { step ->
            val sgfMove = Util.getSGFCoordinates(move)
            val branches = state.node?.branches ?: step.branches
            val branch = branches.find { it.move == sgfMove } ?: branches.find { it.move == "zz" }
            branch?.let { node ->
                moveReplyJob = viewModelScope.launch {
                    var position = RulesManager.makeMove(state.position!!, StoneType.BLACK, move)
                            ?: run {
                                _state.value = state.copy(
                                        hoveredCell = null
                                )
                                return@launch
                            }
                    node.reply?.let {
                        _state.value = state.copy(
                                position = position,
                                boardInteractive = false,
                                hoveredCell = null,
                                removedStones = Util.getRemovedStonesInLastMove(position)
                        )
                        delay(600)
                        position = RulesManager.makeMove(position, StoneType.WHITE, Util.getCoordinatesFromSGF(it))
                                ?: throw RuntimeException("Invalid move $it tutorial = ${state.tutorial?.name}")
                    }
                    _state.value = state.copy(
                            position = position,
                            boardInteractive = !node.success && !node.failed,
                            node = node,
                            hoveredCell = null,
                            removedStones = Util.getRemovedStonesInLastMove(position)
                    )
                }
            }
        }
    }
}