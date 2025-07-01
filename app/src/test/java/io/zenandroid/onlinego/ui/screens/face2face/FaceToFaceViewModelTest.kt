package io.zenandroid.onlinego.ui.screens.face2face

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.di.allKoinModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class FaceToFaceViewModelTest {
  @get:Rule
  val koinTestRule = KoinTestRule.create {
    printLogger(Level.DEBUG)
    modules(allKoinModules)
  }

  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  private val prefs: SharedPreferences = mock()
  private val analytics: FirebaseAnalytics = mock()
  private val crashlytics: FirebaseCrashlytics = mock()

  private lateinit var viewModel: FaceToFaceViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(StandardTestDispatcher())
    viewModel = FaceToFaceViewModel(
      analytics = analytics,
      crashlytics = crashlytics,
      prefs = prefs,
      testing = true
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `smoke test`() {
    runTest {
      moleculeFlow(RecompositionMode.Immediate) {
        viewModel.molecule()
      }.test {
        skipItems(1)

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3)))

        skipItems(1)
        var item = awaitItem()
        Assert.assertEquals(1, item.position?.blackStones?.size)
        Assert.assertEquals(0, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.history.size)
        Assert.assertEquals(StoneType.WHITE, item.position?.nextToMove)
        Assert.assertEquals(6.5f, item.position?.komi)

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2)))

        skipItems(1)
        item = awaitItem()
        Assert.assertEquals(1, item.position?.blackStones?.size)
        Assert.assertEquals(1, item.position?.whiteStones?.size)
        Assert.assertEquals(2, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(2, 2)))

        skipItems(1)
        item = awaitItem()
        Assert.assertEquals(2, item.position?.blackStones?.size)
        Assert.assertEquals(1, item.position?.whiteStones?.size)
        Assert.assertEquals(3, item.history.size)
        Assert.assertEquals(StoneType.WHITE, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(2, 3)))
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(4, 2)))
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 4)))
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 1))) // capture move
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(4, 3)))
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(5, 2)))
        skipItems(2)
        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2))) // capture move

        skipItems(1)
        item = awaitItem()
        Assert.assertEquals(4, item.position?.blackStones?.size)
        Assert.assertEquals(4, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.position?.whiteCaptureCount)
        Assert.assertEquals(1, item.position?.blackCaptureCount)
        Assert.assertEquals(10, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)

        viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3))) // KO attempt
        item = awaitItem()
        Assert.assertEquals(4, item.position?.blackStones?.size)
        Assert.assertEquals(4, item.position?.whiteStones?.size)
        Assert.assertEquals(1, item.position?.whiteCaptureCount)
        Assert.assertEquals(1, item.position?.blackCaptureCount)
        Assert.assertEquals(10, item.history.size)
        Assert.assertEquals(StoneType.BLACK, item.position?.nextToMove)
        Assert.assertEquals(true, item.koMoveDialogShowing)

        viewModel.onAction(Action.KOMoveDialogDismiss)
        item = awaitItem()
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
        skipItems(1)

        viewModel.onAction(Action.NewGameParametersChanged(GameParameters(BoardSize.SMALL, 0)))
        skipItems(1)
        viewModel.onAction(Action.StartNewGame)
        var item = awaitItem()
        skipItems(1)
        moves.dropLast(1).forEach { cell ->
          viewModel.onAction(Action.BoardCellTapUp(cell))
          skipItems(2)
        }
        viewModel.onAction(Action.BoardCellTapUp(moves.last()))
        item = awaitItem()
        Assert.assertEquals(item.koMoveDialogShowing, true)
        Assert.assertEquals(item.history, moves.dropLast(1))
        cancel()
      }
    }
  }
}