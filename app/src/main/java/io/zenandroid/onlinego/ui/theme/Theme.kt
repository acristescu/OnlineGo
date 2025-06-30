package io.zenandroid.onlinego.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
fun OnlineGoTheme(darkTheme: Boolean = isSystemInDarkTheme(), m3: Boolean = false, content: @Composable () -> Unit) {
    if (m3) {
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
    } else {
        val colors = if (darkTheme) {
            DarkColorPalette
        } else {
            LightColorPalette
        }

        androidx.compose.material.MaterialTheme(
            colors = colors,
            typography = typography,
            shapes = shapes
        ) {
            content()
        }
    }
}

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
