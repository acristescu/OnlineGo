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
import io.zenandroid.onlinego.data.model.local.TutorialStep.Interactive
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
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
                        text = step.text,
                        node = null,
                        page = null,
                        retryButtonVisible = true,
                        nextButtonVisible = false,
                        boardInteractive = step.branches.isNotEmpty()
                )
            }
            is TutorialStep.Lesson -> {
                val initialPage = step.pages[0]
                loadPage(tutorial, step, initialPage)
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
                if(state.page != null) {
                    val step = state.step as TutorialStep.Lesson
                    val nextPageIndex = step.pages.indexOf(state.page) + 1
                    if(nextPageIndex >= step.pages.size) {
                        onStepDone()
                    } else {
                        loadPage(state.tutorial, step, step.pages[nextPageIndex])
                    }
                } else {
                    onStepDone()
                }
            }
        }
    }

    private fun loadPage(tutorial: Tutorial?, step: TutorialStep.Lesson, page: Page) {
        val pos = Position(step.size).apply {
            populateWithSGF(page.position)
            page.marks?.let { populateWithMarks(page.marks) }
        }
        _state.value = _state.value.copy(
                tutorialGroups = tutorialsRepository.getTutorialGroups(),
                tutorial = tutorial,
                step = step,
                page = page,
                text = page.text,
                position = pos,
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
                                hoveredCell = null
                        )
                        delay(600)
                        position = RulesManager.makeMove(position, StoneType.WHITE, Util.getCoordinatesFromSGF(it))
                                ?: throw RuntimeException("Invalid move $it tutorial = ${state.tutorial?.name}")
                    }
                    _state.value = state.copy(
                            position = position,
                            boardInteractive = !node.success && !node.failed,
                            node = node,
                            hoveredCell = null
                    )
                }
            }
        }
    }
}