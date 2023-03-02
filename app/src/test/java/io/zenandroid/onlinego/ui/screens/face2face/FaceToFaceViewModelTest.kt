package io.zenandroid.onlinego.ui.screens.face2face

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import io.zenandroid.onlinego.di.allKoinModules
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
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

  val viewModel = FaceToFaceViewModel(testing = true)

  @Test
  fun myTest() {
    runTest {
      moleculeFlow(RecompositionClock.Immediate) {
        viewModel.Molecule()
      }.test {
        Assert.assertEquals(true, awaitItem().loading)
        viewModel.loading = false
        Assert.assertEquals(false, awaitItem().loading)
        viewModel.loading = true
        Assert.assertEquals(true, awaitItem().loading)
        cancel()
      }
    }
  }

}