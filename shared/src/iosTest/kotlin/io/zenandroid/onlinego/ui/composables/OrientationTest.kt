package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrientationTest {

  @Test
  fun onlyAWindowWiderThanItIsTallIsLandscape() = runComposeUiTest {
    val sizes = listOf(IntSize(400, 800), IntSize(800, 400), IntSize(500, 500))
    val results = mutableListOf<Boolean>()
    setContent {
      sizes.forEach { size ->
        val window = object : WindowInfo {
          override val isWindowFocused = true
          override val containerSize = size
        }
        CompositionLocalProvider(LocalWindowInfo provides window) {
          results += isLandscape()
        }
      }
    }
    waitForIdle()
    assertEquals(listOf(false, true, false), results.take(3))
  }
}
