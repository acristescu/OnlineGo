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
import androidx.compose.ui.platform.LocalContext
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.ic_stone_black_svg
import io.zenandroid.onlinego.resources.ic_stone_white_svg
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.vectorResource

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
      if (darkTheme) boardTheme.style.backgroundImageDarkMode else boardTheme.style.backgroundImage
    CompositionLocalProvider(
      LocalThemeSettings provides ThemeSettings(
        isDarkTheme = darkTheme,
        boardTheme = boardTheme,
        dynamicColors = true,
        showCoordinates = true,
      ),
      LocalPreloadedImages provides PreloadedImages(
        background = backgroundResId?.let { imageResource(it) },
        whiteStone = vectorResource(Res.drawable.ic_stone_white_svg),
        blackStone = vectorResource(Res.drawable.ic_stone_black_svg),
      ),
    ) {
      content()
    }
  }
}
