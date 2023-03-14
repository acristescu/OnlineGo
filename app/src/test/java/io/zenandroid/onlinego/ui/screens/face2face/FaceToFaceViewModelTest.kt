package io.zenandroid.onlinego.ui.screens.face2face

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
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
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class FaceToFaceViewModelTest {
  @get:Rule
  val koinTestRule = KoinTestRule.create {
    printLogger(Level.DEBUG)
    modules(allKoinModules)
  }

  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  private val settingsRepository: SettingsRepository = mock {
    whenever(it.boardTheme).thenReturn(BoardTheme.WOOD)
    whenever(it.showCoordinates).thenReturn(true)
  }

  private val prefs: SharedPreferences = mock {}

  private lateinit var viewModel: FaceToFaceViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(StandardTestDispatcher())
    viewModel = FaceToFaceViewModel(
      settingsRepository = settingsRepository,
      prefs = prefs,
      testing = true
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `when the viewmodel starts, loading is true`() {
    runTest {
      moleculeFlow(RecompositionClock.Immediate) {
        viewModel.molecule()
      }.test {
        Assert.assertEquals(true, awaitItem().loading)
        skipItems(1)
        verify(prefs).contains(any())
        Assert.assertEquals(false, awaitItem().loading)
        cancel()
      }
    }
  }

  @Test
  fun `smoke test`() {
    runTest {
      moleculeFlow(RecompositionClock.Immediate) {
        viewModel.molecule()
      }.test {
        skipItems(3)

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
}