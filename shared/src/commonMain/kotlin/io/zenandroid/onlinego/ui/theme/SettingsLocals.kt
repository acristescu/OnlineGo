package io.zenandroid.onlinego.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.ic_stone_black_svg
import io.zenandroid.onlinego.resources.ic_stone_white_svg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

data class ThemeSettings(
  val isDarkTheme: Boolean,
  val boardTheme: BoardTheme,
  val dynamicColors: Boolean,
  val showCoordinates: Boolean,
)

data class PreloadedImages(
  val background: ImageBitmap?,
  val whiteStone: ImageVector?,
  val blackStone: ImageVector?,
)

val LocalThemeSettings = staticCompositionLocalOf {
  ThemeSettings(
    isDarkTheme = false,
    boardTheme = BoardTheme.WOOD,
    dynamicColors = true,
    showCoordinates = true,
  )
}

val LocalPreloadedImages = staticCompositionLocalOf<PreloadedImages?> { null }

suspend fun loadPreloadedImages(
  boardTheme: BoardTheme,
  isDarkTheme: Boolean,
  density: Density,
): PreloadedImages = withContext(Dispatchers.Default) {
  val style = boardTheme.style
  val background = if (isDarkTheme) style.backgroundImageDarkMode else style.backgroundImage
  PreloadedImages(
    background = background?.let { readDrawable(it).decodeToImageBitmap() },
    whiteStone = readDrawable(Res.drawable.ic_stone_white_svg).decodeToImageVector(density),
    blackStone = readDrawable(Res.drawable.ic_stone_black_svg).decodeToImageVector(density),
  )
}

private suspend fun readDrawable(resource: DrawableResource) =
  getDrawableResourceBytes(getSystemResourceEnvironment(), resource)
