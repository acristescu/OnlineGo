package io.zenandroid.onlinego.ui.screens.tutorial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Page
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.data.model.local.TutorialStep.GameExample
import io.zenandroid.onlinego.data.model.local.TutorialStep.Interactive
import io.zenandroid.onlinego.data.model.local.TutorialStep.Lesson
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.decodeSGF
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.BoardCellHovered
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.BoardCellTapped
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.NextPressed
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.RetryPressed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TutorialViewModel(
  private val tutorialsRepository: TutorialsRepository,
  saveStateHandle: SavedStateHandle
) : ViewModel() {
  private val _state = MutableStateFlow(initialState())

  val state: StateFlow<TutorialState> = _state
  var moveReplyJob: Job? = null

  init {
    loadTutorial(saveStateHandle["tutorialName"] ?: error("No tutorial name provided"))
  }

  private fun initialState() = TutorialState()

  fun loadTutorial(tutorialName: String) {
    viewModelScope.launch {
      val tutorial = tutorialsRepository.loadTutorial(tutorialName)!!
      val step = tutorial.steps[0]
      loadStep(tutorial, step)
    }
  }

  private fun loadStep(tutorial: Tutorial, step: TutorialStep) {
    when (step) {
      is Interactive -> {
        viewModelScope.launch {
          _state.update {
            it.copy(
              tutorialGroups = tutorialsRepository.getTutorialGroups(),
              tutorial = tutorial,
              step = step,
              position = decodeSGF(step.size, step.size, step.init, null, null),
              removedStones = null,
              text = step.text,
              node = null,
              page = null,
              retryButtonVisible = true,
              nextButtonVisible = false,
              boardInteractive = step.branches.isNotEmpty()
            )
          }
        }
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

  fun onAction(action: TutorialAction) {
    when (action) {
      is BoardCellHovered -> _state.update {
        it.copy(
          hoveredCell = if (it.position?.getStoneAt(action.point) != null) null else action.point
        )
      }

      is BoardCellTapped -> makeMove(state.value, action.point)
      RetryPressed -> {
        moveReplyJob?.cancel()
        loadStep(state.value.tutorial!!, state.value.step!!)
      }

      NextPressed -> {
        val currentState = state.value
        when (currentState.step) {
          is Lesson -> {
            val nextPageIndex = currentState.step.pages.indexOf(currentState.page) + 1
            if (nextPageIndex >= currentState.step.pages.size) {
              onStepDone()
            } else {
              loadPage(
                currentState.tutorial,
                currentState.step,
                currentState.step.pages[nextPageIndex]
              )
            }
          }

          is GameExample -> {
            if (currentState.gameExamplePositionIndex >= currentState.gameExamplePositions.size - 1) {
              onStepDone()
            } else {
              _state.update {
                it.copy(
                  gameExamplePositionIndex = it.gameExamplePositionIndex + 1,
                  position = it.gameExamplePositions[it.gameExamplePositionIndex + 1],
                  removedStones = null
                )
              }
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
    _state.update {
      it.copy(
        gameExamplePositions = positions,
        gameExamplePositionIndex = 0,
        position = positions[0],
        removedStones = null,
        text = game.text,
        step = game
      )
    }
  }

  private fun loadPage(tutorial: Tutorial?, step: Lesson, page: Page) {
    viewModelScope.launch {
      val pos = decodeSGF(step.size, step.size, page.position, page.areas, page.marks)
      _state.update {
        it.copy(
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
    }
  }

  private fun onStepDone() {
    val state = _state.value
    state.tutorial?.let {
      val nextStepIndex = it.steps.indexOf(state.step) + 1
      if (nextStepIndex >= it.steps.size) {
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
        groups.find { it.tutorials.contains(tutorial) }?.let { parent ->
          val nextTutorial = if (parent.tutorials.last() == tutorial) {
            // finished a tutorial group
            if (groups.last() == parent) {
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

  private fun makeMove(state: TutorialState, move: Cell) {
    (state.step as? Interactive)?.let { step ->
      val sgfMove = Util.getSGFCoordinates(move)
      val branches = state.node?.branches ?: step.branches
      val branch = branches.find { it.move == sgfMove } ?: branches.find { it.move == "zz" }
      branch?.let { node ->
        moveReplyJob = viewModelScope.launch {
          var position = RulesManager.makeMove(state.position!!, StoneType.BLACK, move)
            ?: run {
              _state.update {
                it.copy(
                  hoveredCell = null
                )
              }
              return@launch
            }
          node.reply?.let {
            _state.update {
              it.copy(
                position = position,
                boardInteractive = false,
                hoveredCell = null,
                removedStones = Util.getRemovedStones(state.position, position)
              )
            }
            delay(600)
            position =
              RulesManager.makeMove(position, StoneType.WHITE, Util.getCoordinatesFromSGF(it))
                ?: throw RuntimeException("Invalid move $it tutorial = ${state.tutorial?.name}")
          }
          _state.update {
            it.copy(
              position = position,
              boardInteractive = !node.success && !node.failed,
              node = node,
              hoveredCell = null,
              removedStones = Util.getRemovedStones(state.position, position)
            )
          }
        }
      }
    }
  }
}