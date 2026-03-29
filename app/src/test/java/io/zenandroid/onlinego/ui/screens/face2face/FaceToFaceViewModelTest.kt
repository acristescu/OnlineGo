package io.zenandroid.onlinego.ui.screens.face2face

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanConnectionManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FaceToFaceViewModelTest {
  @get:Rule
  val koinTestRule = KoinTestRule.create {
    printLogger(Level.DEBUG)
    modules(allKoinModules)
  }

  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  private val analytics: FirebaseAnalytics = mock()
  private val crashlytics: FirebaseCrashlytics = mock()
  private val settingsRepository: SettingsRepository = mock()
  private val sessionEngine = FaceToFaceSessionEngine()
  private val lanConnectionManager = FaceToFaceLanConnectionManager()

  private lateinit var applicationTestScope: TestScope

  private lateinit var viewModel: FaceToFaceViewModel

  @Before
  fun setUp() {
    val testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    applicationTestScope = TestScope(testDispatcher)

    whenever(settingsRepository.faceToFaceHistoryFlow).thenReturn(flowOf(null))
    whenever(settingsRepository.faceToFaceBoardSizeFlow).thenReturn(flowOf(null))
    whenever(settingsRepository.faceToFaceHandicapFlow).thenReturn(flowOf(null))

    viewModel = FaceToFaceViewModel(
      analytics = analytics,
      crashlytics = crashlytics,
      settingsRepository = settingsRepository,
      sessionEngine = sessionEngine,
      lanConnectionManager = lanConnectionManager,
      applicationScope = applicationTestScope,
      testing = true
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    applicationTestScope.cancel()
  }

  @Test
  fun `smoke test`() {
    runTest {
      moleculeFlow(RecompositionMode.Immediate) {
        viewModel.molecule()
      }.test {
        awaitState { !it.loading }

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3)))

        var item = awaitState { it.history.size == 1 }
        Assert.assertEquals(1, item.position?.blackStones?.size)
        Assert.assertEquals(0, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.history.size)
        Assert.assertEquals(StoneType.WHITE, item.position?.nextToMove)
        Assert.assertEquals(6.5f, item.position?.komi)

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2)))

        item = awaitState { it.history.size == 2 }
        Assert.assertEquals(1, item.position?.blackStones?.size)
        Assert.assertEquals(1, item.position?.whiteStones?.size)
        Assert.assertEquals(2, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(2, 2)))

        item = awaitState { it.history.size == 3 }
        Assert.assertEquals(2, item.position?.blackStones?.size)
        Assert.assertEquals(1, item.position?.whiteStones?.size)
        Assert.assertEquals(3, item.history.size)
        Assert.assertEquals(StoneType.WHITE, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(2, 3)))
        awaitState { it.history.size == 4 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(4, 2)))
        awaitState { it.history.size == 5 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 4)))
        awaitState { it.history.size == 6 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 1))) // capture move
        awaitState { it.history.size == 7 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(4, 3)))
        awaitState { it.history.size == 8 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(5, 2)))
        awaitState { it.history.size == 9 }
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2))) // capture move

        item = awaitState { it.history.size == 10 }
        Assert.assertEquals(4, item.position?.blackStones?.size)
        Assert.assertEquals(4, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.position?.whiteCaptureCount)
        Assert.assertEquals(1, item.position?.blackCaptureCount)
        Assert.assertEquals(10, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3))) // KO attempt
        item = awaitState { it.koMoveDialogShowing }
        Assert.assertEquals(4, item.position?.blackStones?.size)
        Assert.assertEquals(4, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.position?.whiteCaptureCount)
        Assert.assertEquals(1, item.position?.blackCaptureCount)
        Assert.assertEquals(10, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)
        Assert.assertEquals(true, item.koMoveDialogShowing)

        viewModel.onAction(Action.KOMoveDialogDismiss)
        item = awaitState { !it.koMoveDialogShowing }
        Assert.assertEquals(false, item.koMoveDialogShowing)

        cancel()
      }
    }
  }


  @Test
  fun `ko is recognized`() {
    val moves = "E5, D6, E7, E8, E6, F6, F7, D7, G6, F8, F5, F4, G5, H5, E4, H6, G4, H4, F3, D5, E3, D3, D4, C4, C6, E2, C5, F2, C7, C8, D8, D9, D7, B7, B8, B6, C9, B9, E9, F9, G8, H8, G9, H9, H7, J7, J8, J9, J8, J6, A9, B5, B4, A3, A4, B3, A5, A6, A7, A8, A7"
      .split(", ")
      .map { Cell.fromGTP(it, 9) }

    runTest {
      moleculeFlow(RecompositionMode.Immediate) {
        viewModel.molecule()
      }.test {
        awaitState { !it.loading }

        viewModel.onAction(Action.NewGameParametersChanged(GameParameters(BoardSize.SMALL, 0)))
        awaitState { it.newGameParameters == GameParameters(BoardSize.SMALL, 0) }
        viewModel.onAction(Action.StartNewGame)
        awaitState {
          !it.loading &&
            it.history.isEmpty() &&
            it.currentGameParameters == GameParameters(BoardSize.SMALL, 0)
        }
        moves.dropLast(1).forEachIndexed { index, cell ->
          viewModel.onAction(Action.BoardCellTapUp(cell))
          awaitState { it.history.size == index + 1 }
        }
        viewModel.onAction(Action.BoardCellTapUp(moves.last()))
        val item = awaitState { it.koMoveDialogShowing }
        Assert.assertEquals(item.koMoveDialogShowing, true)
        Assert.assertEquals(item.history, moves.dropLast(1))
        cancel()
      }
    }
  }

  private suspend fun ReceiveTurbine<FaceToFaceState>.awaitState(
    predicate: (FaceToFaceState) -> Boolean
  ): FaceToFaceState {
    while (true) {
      val item = awaitItem()
      if (predicate(item)) {
        return item
      }
    }
  }
}
