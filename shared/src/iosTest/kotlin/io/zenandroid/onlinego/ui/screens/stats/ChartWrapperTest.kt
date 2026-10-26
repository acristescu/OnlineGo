package io.zenandroid.onlinego.ui.screens.stats

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_YEAR
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ChartWrapperTest {

  private val chartData = listOf(1773576000f to 1500f, 1780315200f to 1620f)

  @Test
  fun summarisesTheRangeWithALocalisedDate() = runComposeUiTest {
    setContent {
      ChartWrapper(
        chartData = chartData,
        filter = ONE_YEAR,
        collapseTimeByGame = false,
        onFilterChanged = {})
    }

    onNodeWithText("⬆ 120 ELO since ", substring = true).assertTextContains(
      "2026",
      substring = true
    )
  }

  @Test
  fun showsThePointUnderTheFingerUntilRelease() = runComposeUiTest {
    setContent {
      ChartWrapper(
        chartData = chartData,
        filter = ONE_YEAR,
        collapseTimeByGame = false,
        onFilterChanged = {})
    }

    onRoot().performTouchInput { down(Offset(1f, centerY)) }
    onNodeWithText("1500 ELO", substring = true).assertIsDisplayed()

    onRoot().performTouchInput { up() }
    onNodeWithText("⬆ 120 ELO", substring = true).assertIsDisplayed()
  }
}
