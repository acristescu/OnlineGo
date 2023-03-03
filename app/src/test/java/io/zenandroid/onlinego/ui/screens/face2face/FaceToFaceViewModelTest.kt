package io.zenandroid.onlinego.ui.screens.face2face

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.di.allKoinModules
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext.get
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule

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

  val viewModel = FaceToFaceViewModel(
    settingsRepository = settingsRepository,
    testing = true
  )

  @Test
  fun myTest() {
    runTest {
      moleculeFlow(RecompositionClock.Immediate) {
        viewModel.Molecule()
      }.test {
        Assert.assertEquals(true, awaitItem().loading)
        cancel()
      }
    }
  }

}