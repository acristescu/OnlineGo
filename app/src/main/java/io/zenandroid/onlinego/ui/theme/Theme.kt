package io.zenandroid.onlinego.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.ui.screens.main.PreloadedImages
import io.zenandroid.onlinego.ui.screens.main.ThemeSettings

@Composable
fun OnlineGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyM3,
        shapes = shapesM3,
        content = content
    )
}

/**
 * [OnlineGoTheme] plus [LocalThemeSettings]/[LocalPreloadedImages], for @Preview composables.
 * MainActivity is the only place that normally provides those locals (loaded once in
 * onCreate), so any @Preview that renders a [io.zenandroid.onlinego.ui.composables.Board]
 * without this wrapper gets a board with no background image and no stones.
 */
@Composable
fun OnlineGoPreviewTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  boardTheme: BoardTheme = BoardTheme.WOOD,
  content: @Composable () -> Unit,
) {
  OnlineGoTheme(darkTheme) {
    val backgroundResId =
      if (darkTheme) boardTheme.backgroundImageDarkMode else boardTheme.backgroundImage
    CompositionLocalProvider(
      LocalThemeSettings provides ThemeSettings(
        isDarkTheme = darkTheme,
        boardTheme = boardTheme,
        dynamicColors = true,
        showCoordinates = true,
      ),
      LocalPreloadedImages provides PreloadedImages(
        background = backgroundResId?.let { ImageBitmap.imageResource(id = it) },
        whiteStone = ImageVector.vectorResource(id = boardTheme.whiteStone),
        blackStone = ImageVector.vectorResource(id = boardTheme.blackStone),
      ),
    ) {
      content()
    }
  }
}
