package io.zenandroid.onlinego.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val LightColorPalette = lightColors(
    primary = salmon,
    primaryVariant = accent,
    secondary = brownMedium,
    secondaryVariant = salmon,
    background = background,
    onSurface = brown,
    onBackground = brown.copy(alpha = 0.8f),
    onPrimary = lightOnPrimary,
    onSecondary = lightOnPrimary,

    /* Other default colors to override
background = Color.White,
surface = Color.White,
onPrimary = Color.White,
onSecondary = Color.Black,
onBackground = Color.Black,
onSurface = Color.Black,
*/
)

private val DarkColorPalette = darkColors(
    primary = nightBlue,
    primaryVariant = darkBlue,
    secondary = salmon,
    secondaryVariant = darkBlue,
    surface = nightSurface,
    onSurface = nightOnSurface,
    background = nightBackground,
    onBackground = nightOnBackground,
    onPrimary = nightOnPrimary,
    onSecondary = nightOnPrimary,
)

@Composable
fun OnlineGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes
    ) {
        Box(modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues())) {
            content()
        }
    }}

private val LightColorPaletteM3 = lightColorScheme(
    primary = salmon,
    secondary = brownMedium,
    background = background,
    onSurface = brown,
    onBackground = brown.copy(alpha = 0.8f),
    onPrimary = lightOnPrimary,
    onSecondary = lightOnPrimary,
)

private val DarkColorPaletteM3 = darkColorScheme(
    primary = nightBlue,
    secondary = salmon,
    surface = nightSurface,
    onSurface = nightOnSurface,
    background = nightBackground,
    onBackground = nightOnBackground,
    onPrimary = nightOnPrimary,
    onSecondary = nightOnPrimary,
)


@Composable
fun OnlineGoThemeM3(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPaletteM3
    } else {
        LightColorPaletteM3
    }

    androidx.compose.material3.MaterialTheme(
            colorScheme = colors,
            typography = typographyM3,
            shapes = shapesM3,
            content = content
    )
}