package io.zenandroid.onlinego.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.ui.screens.main.PreloadedImages
import io.zenandroid.onlinego.ui.screens.main.ThemeSettings

val LocalThemeSettings = staticCompositionLocalOf {
  ThemeSettings(
    isDarkTheme = false,
    boardTheme = BoardTheme.WOOD,
    dynamicColors = true,
    showCoordinates = true,
  )
}

val LocalPreloadedImages = staticCompositionLocalOf<PreloadedImages?> { null }