package io.zenandroid.onlinego.resources

import androidx.compose.ui.unit.Density
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.test.Test
import kotlin.test.assertTrue

class DrawableResourcesTest {

  private val darkVariants = listOf(
    "art_login_1",
    "ic_board_transparent",
    "logo_ogs",
  )

  @Test
  fun everyDrawableDecodes() = runTest {
    val environment = getSystemResourceEnvironment()
    assertTrue(Res.allDrawableResources.isNotEmpty())
    Res.allDrawableResources.forEach { (name, resource) ->
      decode(name, getDrawableResourceBytes(environment, resource))
    }
  }

  @Test
  fun everyDarkVariantDecodes() = runTest {
    darkVariants.forEach { name ->
      decode("$name (dark)", Res.readBytes("drawable-dark/$name.xml"))
    }
  }

  private fun decode(name: String, bytes: ByteArray) {
    try {
      if (bytes.first() == '<'.code.toByte()) {
        bytes.decodeToImageVector(Density(1f))
      } else {
        bytes.decodeToImageBitmap()
      }
    } catch (e: Exception) {
      throw AssertionError("$name does not decode", e)
    }
  }
}
