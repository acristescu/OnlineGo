package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.rounded._123
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MaterialIconsTest {

  @Test
  fun iconsFromTheFrozenArtifactDraw() = runComposeUiTest {
    val icons =
      listOf(Icons.Filled.BugReport, Icons.AutoMirrored.Rounded.Logout, Icons.Rounded._123)
    setContent {
      Row {
        icons.forEachIndexed { index, icon ->
          Icon(icon, null, Modifier.size(48.dp).testTag("icon$index"), tint = Color.Black)
        }
      }
    }

    icons.indices.forEach { index ->
      val pixels = onNodeWithTag("icon$index").captureToImage().toPixelMap()
      val painted = (0 until pixels.width).sumOf { x ->
        (0 until pixels.height).count { y -> pixels[x, y].alpha > .5f }
      }
      assertTrue(painted > pixels.width * pixels.height / 20, "icon $index painted $painted pixels")
    }
  }
}
