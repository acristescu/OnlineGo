package io.zenandroid.onlinego.ui.composables

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.shared.R
import io.zenandroid.onlinego.ui.theme.style
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

suspend fun renderBoard(
  context: Context,
  boardWidth: Int,
  boardHeight: Int,
  position: Position?,
  boardTheme: BoardTheme,
  isDarkTheme: Boolean,
  sizePx: Int,
): Bitmap {
  val style = boardTheme.style
  val measurements =
    doMeasurements(sizePx, sizePx, boardWidth, boardHeight, drawCoordinates = false)
  val backgroundImage = (if (isDarkTheme) style.backgroundImageDarkMode else style.backgroundImage)
    ?.let { getDrawableResourceBytes(getSystemResourceEnvironment(), it).decodeToImageBitmap() }
  val stoneSize = (measurements.stoneRadius * 2).coerceAtLeast(1)
  fun stonePainter(@DrawableRes id: Int) = BitmapPainter(
    ResourcesCompat.getDrawable(context.resources, id, context.theme)!!
      .toBitmap(stoneSize, stoneSize)
      .asImageBitmap()
  )

  val density = Density(context)

  val bitmap = ImageBitmap(sizePx, sizePx)
  CanvasDrawScope().draw(
    density = density,
    layoutDirection = LayoutDirection.Ltr,
    canvas = Canvas(bitmap),
    size = Size(sizePx.toFloat(), sizePx.toFloat()),
  ) {
    drawBoard(
      boardWidth = boardWidth,
      boardHeight = boardHeight,
      position = position,
      measurements = measurements,
      textMeasurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(context),
        defaultDensity = density,
        defaultLayoutDirection = LayoutDirection.Ltr,
      ),
      boardTheme = boardTheme,
      backgroundImage = backgroundImage,
      whiteStone = stonePainter(R.drawable.ic_stone_white_svg),
      blackStone = stonePainter(R.drawable.ic_stone_black_svg),
      drawCoordinates = false,
      drawShadow = true,
      drawTerritory = false,
      drawLastMove = true,
      drawMarks = true,
      lastMoveMarker = "#",
      fadeOutRemovedStones = false,
    )
  }
  return bitmap.asAndroidBitmap()
}
