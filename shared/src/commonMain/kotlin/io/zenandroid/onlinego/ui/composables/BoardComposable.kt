package io.zenandroid.onlinego.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.gamelogic.RulesManager.isPass
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.theme.LocalPreloadedImages
import io.zenandroid.onlinego.ui.theme.LocalThemeSettings
import io.zenandroid.onlinego.ui.theme.style
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt

private const val TEXT_LAYOUT_CACHE_SIZE = 128
private const val SHADOW_ALPHA = 0xBB / 255f
private val textGlow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 8f)

@Composable
fun Board(
  modifier: Modifier = Modifier,
  boardWidth: Int,
  boardHeight: Int,
  position: Position?,
  candidateMove: Cell? = null,
  candidateMoveType: StoneType? = null,
  boardTheme: BoardTheme = LocalThemeSettings.current.boardTheme,
  drawCoordinates: Boolean = LocalThemeSettings.current.showCoordinates,
  hints: ImmutableList<MoveInfo>? = null,
  ownership: ImmutableList<Float>? = null,
  interactive: Boolean = true,
  drawShadow: Boolean = true,
  drawTerritory: Boolean = false,
  drawLastMove: Boolean = true,
  lastMoveMarker: String = "#",
  fadeInLastMove: Boolean = true,
  fadeOutRemovedStones: Boolean = true,
  removedStones: ImmutableList<Pair<Cell, StoneType>>? = null,
  onTapMove: ((Cell) -> Unit)? = null,
  onTapUp: ((Cell) -> Unit)? = null
) {
  BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {

    val drawMarks = true

    val preloadedImages = LocalPreloadedImages.current
    val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_LAYOUT_CACHE_SIZE)
    val currentOnTapMove by rememberUpdatedState(onTapMove)
    val currentOnTapUp by rememberUpdatedState(onTapUp)

    // Stones images
    val blackStone = preloadedImages?.blackStone?.let { rememberVectorPainter(it) }
    val whiteStone = preloadedImages?.whiteStone?.let { rememberVectorPainter(it) }

    val width = with(LocalDensity.current) { maxWidth.roundToPx() }
    val height = with(LocalDensity.current) { maxHeight.roundToPx() }

    val measurements = remember(width, height, boardWidth, boardHeight, drawCoordinates) {
      doMeasurements(
        width,
        height,
        boardWidth,
        boardHeight,
        drawCoordinates
      )
    }

    var lastHotTrackedPoint: Cell? by remember { mutableStateOf(null) }
    var stoneToFadeIn: Cell? by remember(position?.lastMove) { mutableStateOf(position?.lastMove) }
    var stonesToFadeOut: List<Pair<Cell, StoneType>>? by remember(removedStones) {
      mutableStateOf(
        removedStones
      )
    }

    val fadeInAlpha = remember { Animatable(.4f) }
    val fadeOutAlpha = remember { Animatable(1f) }

    if (fadeInLastMove && stoneToFadeIn != null) {
      LaunchedEffect(stoneToFadeIn) {
        fadeInAlpha.snapTo(.4f)
        fadeInAlpha.animateTo(1f, animationSpec = tween(150))
        stoneToFadeIn = null
      }
    }
    if (fadeOutRemovedStones && stonesToFadeOut?.isNotEmpty() == true) {
      LaunchedEffect(stonesToFadeOut) {
        fadeOutAlpha.snapTo(1f)
        fadeOutAlpha.animateTo(0f, animationSpec = tween(150, 75))
        stonesToFadeOut = null
      }
    }
    Canvas(
      modifier = Modifier.fillMaxSize()
        .run {
          if (interactive) {
            pointerInput(measurements, boardWidth, boardHeight) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  val change =
                    event.changes.firstOrNull { it.pressed || it.previousPressed } ?: continue
                  if (measurements.cellSize == 0) {
                    continue
                  }
                  val eventCoords = screenToBoardCoordinates(
                    change.position.x,
                    change.position.y,
                    measurements,
                    boardWidth,
                    boardHeight
                  )

                  if (eventCoords != lastHotTrackedPoint) {
                    currentOnTapMove?.invoke(eventCoords)
                    lastHotTrackedPoint = eventCoords
                  }

                  if (event.changes.none { it.pressed }) {
                    currentOnTapUp?.invoke(eventCoords)
                    lastHotTrackedPoint = null
                  }
                  event.changes.forEach { it.consume() }
                }
              }
            }
          } else {
            this
          }
        }
    ) {
      drawBoard(
        boardWidth = boardWidth,
        boardHeight = boardHeight,
        position = position,
        measurements = measurements,
        textMeasurer = textMeasurer,
        boardTheme = boardTheme,
        backgroundImage = preloadedImages?.background,
        whiteStone = whiteStone,
        blackStone = blackStone,
        drawCoordinates = drawCoordinates,
        drawShadow = drawShadow,
        drawTerritory = drawTerritory,
        drawLastMove = drawLastMove,
        drawMarks = drawMarks,
        lastMoveMarker = lastMoveMarker,
        fadeOutRemovedStones = fadeOutRemovedStones,
        stoneToFadeIn = if (fadeInLastMove) stoneToFadeIn else null,
        fadeInAlpha = fadeInAlpha.value,
        stonesToFadeOut = stonesToFadeOut,
        fadeOutAlpha = fadeOutAlpha.value,
        ownership = ownership,
        hints = hints,
        candidateMove = candidateMove,
        candidateMoveType = candidateMoveType,
      )
    }
  }
}

internal fun DrawScope.drawBoard(
  boardWidth: Int,
  boardHeight: Int,
  position: Position?,
  measurements: Measurements,
  textMeasurer: TextMeasurer,
  boardTheme: BoardTheme,
  backgroundImage: ImageBitmap?,
  whiteStone: Painter?,
  blackStone: Painter?,
  drawCoordinates: Boolean,
  drawShadow: Boolean,
  drawTerritory: Boolean,
  drawLastMove: Boolean,
  drawMarks: Boolean,
  lastMoveMarker: String,
  fadeOutRemovedStones: Boolean,
  stoneToFadeIn: Cell? = null,
  fadeInAlpha: Float = 1f,
  stonesToFadeOut: List<Pair<Cell, StoneType>>? = null,
  fadeOutAlpha: Float = 1f,
  ownership: ImmutableList<Float>? = null,
  hints: ImmutableList<MoveInfo>? = null,
  candidateMove: Cell? = null,
  candidateMoveType: StoneType? = null,
) {
  drawBackground(backgroundImage, boardTheme.style.backgroundColor)
  translate(
    measurements.border + measurements.xOffsetForNonSquareBoard,
    measurements.border + measurements.yOffsetForNonSquareBoard
  ) {

    drawGrid(
      boardWidth,
      boardHeight,
      candidateMove,
      boardTheme.style.textAndGridColor,
      measurements
    )
    drawStarPoints(boardWidth, boardHeight, boardTheme.style.textAndGridColor, measurements)
    drawCoordinates(
      boardWidth,
      boardHeight,
      boardTheme,
      drawCoordinates,
      measurements,
      textMeasurer
    )

    for (item in stonesToFadeOut ?: emptyList()) {
      drawStone(
        item.first,
        item.second,
        if (item.second == StoneType.WHITE) whiteStone else blackStone,
        fadeOutAlpha,
        true,
        measurements
      )
    }
    position?.let {
      for (p in position.whiteStones) {
        val alpha = when {
          p == stoneToFadeIn -> fadeInAlpha
          fadeOutRemovedStones && it.removedSpots.contains(p) -> .4f
          else -> 1f
        }
        drawStone(p, StoneType.WHITE, whiteStone, alpha, drawShadow, measurements)
      }
      for (p in position.blackStones) {
        val alpha = when {
          p == stoneToFadeIn -> fadeInAlpha
          fadeOutRemovedStones && it.removedSpots.contains(p) -> .4f
          else -> 1f
        }
        drawStone(p, StoneType.BLACK, blackStone, alpha, drawShadow, measurements)
      }

      drawDecorations(it, drawLastMove, drawMarks, lastMoveMarker, measurements, textMeasurer)

      if (drawTerritory) {
        drawTerritory(it, measurements)
      }

      ownership?.let { ownership ->
        drawAiEstimatedOwnership(it, ownership, measurements)
      }

      hints?.let { hints ->
        drawHints(it, hints, measurements, textMeasurer)
      }
    }
    candidateMove?.let {
      drawStone(
        it,
        candidateMoveType,
        if (candidateMoveType == StoneType.WHITE) whiteStone else blackStone,
        .4f,
        false,
        measurements
      )
    }
  }
}

private fun screenToBoardCoordinates(
  x: Float,
  y: Float,
  measurements: Measurements,
  boardWidth: Int,
  boardHeight: Int
): Cell {
  return Cell(
    ((x - measurements.border - measurements.xOffsetForNonSquareBoard).toInt() / measurements.cellSize).coerceIn(
      0,
      boardWidth - 1
    ),
    ((y - measurements.border - measurements.yOffsetForNonSquareBoard).toInt() / measurements.cellSize).coerceIn(
      0,
      boardHeight - 1
    )
  )
}

private val coordinatesX = arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
private val coordinatesY = (1..25).map(Int::toString)

@Immutable
internal data class Measurements(
  var width: Int,
  val cellSize: Int,
  val border: Float,
  val linesWidth: Float,
  val highlightLinesWidth: Float,
  val decorationsLineWidth: Float,
  val stoneSpacing: Float,
  val textSize: Float,
  val coordinatesTextSize: Float,
  val stoneShadow: Brush,
  val stoneShadowRadius: Float,
  val stoneShadowOffset: Float,
  val halfCell: Float,
  val stoneRadius: Int,
  val xOffsetForNonSquareBoard: Float,
  val yOffsetForNonSquareBoard: Float,
  val centers: List<List<Offset>>,
)

private fun getCellCenter(i: Int, j: Int, measurements: Measurements) =
  measurements.centers[i][j]

private fun DrawScope.drawSingleStarPoint(
  i: Int,
  j: Int,
  color: Color,
  measurements: Measurements
) {
  val center = getCellCenter(i, j, measurements)
  drawCircle(color, measurements.cellSize / 15f, Offset(center.x, center.y))
}

private fun DrawScope.drawTextCentred(
  textMeasurer: TextMeasurer,
  text: String,
  cx: Float,
  cy: Float,
  textSize: Float,
  color: Color = Color.Black,
  shadow: Shadow? = textGlow,
  ignoreAscentDescent: Boolean = false,
) {
  val layout = textMeasurer.measure(
    text = text,
    style = TextStyle(fontSize = textSize.toSp()),
    layoutDirection = layoutDirection,
    density = this,
  )
  val ink = textInkBounds(text, textSize)
  drawText(
    textLayoutResult = layout,
    color = color,
    topLeft = Offset(
      cx - ink.center.x,
      cy - ink.center.y - layout.firstBaseline + if (ignoreAscentDescent) ink.bottom / 2 else 0f,
    ),
    shadow = shadow,
  )
}

internal expect fun textInkBounds(text: String, textSize: Float): Rect

private fun DrawScope.drawBackground(backgroundImage: ImageBitmap?, backgroundColor: Color?) {
  if (backgroundImage != null) {
    drawImage(
      image = backgroundImage,
      dstSize = IntSize(ceil(this.size.width).toInt(), ceil(this.size.height).toInt())
    )
  }
  if (backgroundColor != null) {
    val size = Size(ceil(this.size.width), ceil(this.size.height))
    drawRect(color = backgroundColor, size = size)
  }
}

private fun DrawScope.drawAiEstimatedOwnership(
  pos: Position,
  ownership: List<Float>,
  measurements: Measurements
) {
  for (i in 0 until pos.boardWidth) {
    for (j in 0 until pos.boardHeight) {
      val center = getCellCenter(i, j, measurements)
      val fillColor = when {
        ownership[i + j * pos.boardWidth] > 0 -> Color.White
        ownership[i + j * pos.boardWidth] < 0 -> Color.Black
        else -> Color.Transparent
      }

      drawRoundRect(
        color = fillColor,
        topLeft = Offset(
          x = center.x - (measurements.cellSize / 8).toFloat(),
          y = center.y - (measurements.cellSize / 8).toFloat(),
        ),
        size = Size(
          width = measurements.cellSize / 4f,
          height = measurements.cellSize / 4f,
        ),
        alpha = abs(ownership[i + j * pos.boardWidth]),
        style = Fill,
        cornerRadius = CornerRadius(measurements.cellSize / 16f, measurements.cellSize / 16f),
      )
    }
  }
}

private fun DrawScope.drawHints(
  position: Position,
  hints: List<MoveInfo>,
  measurements: Measurements,
  textMeasurer: TextMeasurer,
) {
  for ((index, hint) in hints.take(5).withIndex()) {
    val coords = Util.getCoordinatesFromGTP(hint.move, position.boardHeight)
    if (!coords.isPass) {
      val center = getCellCenter(coords.x, coords.y, measurements)
      drawCircle(
        Color.Black,
        measurements.cellSize / 2f - measurements.stoneSpacing,
        Offset(center.x, center.y),
        style = Stroke(measurements.decorationsLineWidth),
      )

      drawTextCentred(
        textMeasurer,
        (index + 1).toString(),
        center.x,
        center.y,
        textSize = measurements.textSize
      )
    }
  }
}


private fun DrawScope.drawTerritory(position: Position, measurements: Measurements) {
  for (i in 0 until position.boardWidth) {
    for (j in 0 until position.boardHeight) {
      val p = Cell(i, j)
      val center = getCellCenter(i, j, measurements)

      val fillColor = when {
        position.whiteTerritory.contains(p) -> Color.White
        position.blackTerritory.contains(p) -> Color.Black
        position.getStoneAt(p) == null && position.removedSpots.contains(p) -> Color.Transparent
        else -> continue
      }
      drawRoundRect(
        color = fillColor,
        topLeft = Offset(
          x = center.x - (measurements.cellSize / 8).toFloat(),
          y = center.y - (measurements.cellSize / 8).toFloat(),
        ),
        size = Size(
          width = measurements.cellSize / 4f,
          height = measurements.cellSize / 4f,
        ),
        style = Fill,
        cornerRadius = CornerRadius(measurements.cellSize / 16f, measurements.cellSize / 16f),
      )
      drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(
          x = center.x - (measurements.cellSize / 8).toFloat(),
          y = center.y - (measurements.cellSize / 8).toFloat(),
        ),
        size = Size(
          width = measurements.cellSize / 4f,
          height = measurements.cellSize / 4f,
        ),
        style = Stroke(width = 2f),
        cornerRadius = CornerRadius(measurements.cellSize / 16f, measurements.cellSize / 16f),
      )
    }
  }
}

private fun DrawScope.drawDecorations(
  position: Position,
  drawLastMove: Boolean,
  drawMarks: Boolean,
  lastMoveMarker: String,
  measurements: Measurements,
  textMeasurer: TextMeasurer,
) {
  if (drawLastMove) {
    position.lastMove?.let {
      if (!it.isPass()) {
        val color = if (position.lastPlayerToMove == StoneType.WHITE) Color.Black else Color.White
        if (lastMoveMarker == "#") {
          drawDefaultLastMoveMarker(it, measurements, color)
        } else {
          val center = getCellCenter(it.x, it.y, measurements)
          drawTextCentred(
            textMeasurer,
            lastMoveMarker,
            center.x,
            center.y,
            textSize = measurements.textSize,
            color = color
          )
        }
      }
    }
  }

  position.variation.forEachIndexed { i, p ->
    val center = getCellCenter(p.x, p.y, measurements)
    val stone = position.getStoneAt(p)
    val color = if (stone == null || stone == StoneType.WHITE) Color.Black else Color.White
    drawTextCentred(
      textMeasurer,
      (i + 1).toString(),
      center.x,
      center.y,
      textSize = measurements.textSize,
      color = color
    )
  }

  if (drawMarks) {
    position.customMarks.forEach {
      if (!it.placement.isPass) {
        when (it.text) {
          "#" -> { // last move mark
            val color =
              if (position.getStoneAt(it.placement) == StoneType.WHITE) Color.Black else Color.White
            drawDefaultLastMoveMarker(it.placement, measurements, color)
          }

          else -> {
            val center = getCellCenter(it.placement.x, it.placement.y, measurements)

            val color = when (it.category) {
              PlayCategory.IDEAL -> Color(0xC0D0FFC0)
              PlayCategory.GOOD -> Color(0xC0F0FF90)
              PlayCategory.MISTAKE -> Color(0xC0ED7861)
              PlayCategory.TRICK -> Color(0xC0D384F9)
              PlayCategory.QUESTION -> Color(0xC079B3E4)
              PlayCategory.LABEL -> Color(0x70FFFFFF)
              null -> Color(0x00FFFFFF)
            }


            drawCircle(
              color,
              measurements.cellSize / 2f - measurements.stoneSpacing,
              Offset(center.x, center.y)
            )
            it.text?.let {
              drawTextCentred(
                textMeasurer,
                it,
                center.x,
                center.y,
                textSize = measurements.textSize
              )
            }
          }
        }
      }
    }
  }
}

private fun DrawScope.drawDefaultLastMoveMarker(
  cell: Cell,
  measurements: Measurements,
  color: Color
) {
  val center = getCellCenter(cell.x, cell.y, measurements)
  drawCircle(
    color,
    measurements.cellSize / 4f,
    center,
    style = Stroke(measurements.decorationsLineWidth)
  )
}

private fun DrawScope.drawStone(
  p: Cell,
  type: StoneType?,
  stonePainter: Painter?,
  alpha: Float = 1f,
  drawShadow: Boolean,
  measurements: Measurements
) {
  if (stonePainter == null || p.isPass) {
    return
  }
  if (p.x < 0 || p.y < 0) {
    CrashReporter.recordException(Exception("Invalid cell: $p"))
    return
  }
  val center = getCellCenter(p.x, p.y, measurements)
  if (drawShadow && alpha > .75) {
    translate(center.x, center.y + measurements.stoneShadowOffset) {
      drawCircle(measurements.stoneShadow, measurements.stoneShadowRadius, Offset.Zero)
    }
  }

  if (measurements.cellSize > 40) {
    translate(center.x - measurements.stoneRadius, center.y - measurements.stoneRadius) {
      with(stonePainter) {
        draw(Size(measurements.stoneRadius * 2f, measurements.stoneRadius * 2f), alpha = alpha)
      }
    }
  } else {
    val color = if (type == StoneType.BLACK) Color.Black else Color.White

    drawCircle(
      color = color,
      center = Offset(center.x, center.y),
      radius = measurements.stoneRadius.toFloat(),
      style = Fill
    )
    if (type == StoneType.WHITE) {
      drawCircle(
        color = Color(0xCC331810),
        center = Offset(center.x, center.y),
        radius = measurements.stoneRadius.toFloat(),
        style = Stroke(1f)
      )
    }
  }
}

private fun DrawScope.drawGrid(
  boardWidth: Int,
  boardHeight: Int,
  candidateMove: Cell?,
  textAndGridColor: Color,
  measurements: Measurements,
) {
  for (i in 0 until boardWidth) {
    val start = i * measurements.cellSize + measurements.halfCell
    val fullLength = (measurements.cellSize * boardHeight).toFloat()

    val (color, lineWidth) = if (i == candidateMove?.x) Color.White to measurements.highlightLinesWidth else textAndGridColor to measurements.linesWidth
    drawLine(
      color,
      Offset(start, measurements.halfCell),
      Offset(start, fullLength - measurements.halfCell),
      lineWidth
    )
  }
  for (i in 0 until boardHeight) {
    val start = i * measurements.cellSize + measurements.halfCell
    val fullLength = (measurements.cellSize * boardWidth).toFloat()

    val (color, lineWidth) = if (i == candidateMove?.y) Color.White to measurements.highlightLinesWidth else textAndGridColor to measurements.linesWidth
    drawLine(
      color,
      Offset(measurements.halfCell, start),
      Offset(fullLength - measurements.halfCell, start),
      lineWidth
    )
  }
}

private fun DrawScope.drawStarPoints(
  boardWidth: Int,
  boardHeight: Int,
  color: Color,
  measurements: Measurements
) {
  if (boardWidth == boardHeight) {
    when (boardWidth) {
      19 -> {
        drawSingleStarPoint(3, 3, color, measurements)
        drawSingleStarPoint(15, 15, color, measurements)
        drawSingleStarPoint(3, 15, color, measurements)
        drawSingleStarPoint(15, 3, color, measurements)

        drawSingleStarPoint(3, 9, color, measurements)
        drawSingleStarPoint(9, 3, color, measurements)
        drawSingleStarPoint(15, 9, color, measurements)
        drawSingleStarPoint(9, 15, color, measurements)

        drawSingleStarPoint(9, 9, color, measurements)
      }

      13 -> {
        drawSingleStarPoint(3, 3, color, measurements)
        drawSingleStarPoint(9, 9, color, measurements)
        drawSingleStarPoint(3, 9, color, measurements)
        drawSingleStarPoint(9, 3, color, measurements)

        drawSingleStarPoint(6, 6, color, measurements)
      }

      9 -> {
        drawSingleStarPoint(2, 2, color, measurements)
        drawSingleStarPoint(6, 6, color, measurements)
        drawSingleStarPoint(2, 6, color, measurements)
        drawSingleStarPoint(6, 2, color, measurements)

        drawSingleStarPoint(4, 4, color, measurements)
      }
    }
  }
}

private fun DrawScope.drawCoordinates(
  boardWidth: Int,
  boardHeight: Int,
  boardTheme: BoardTheme,
  drawCoordinates: Boolean,
  measurements: Measurements,
  textMeasurer: TextMeasurer,
) {
  if (drawCoordinates) {
    val textColor = boardTheme.style.textAndGridColor
    for (i in 0 until boardWidth) {
      drawTextCentred(
        textMeasurer,
        coordinatesX[i],
        getCellCenter(i, 0, measurements).x,
        0f - measurements.border / 2,
        textSize = measurements.coordinatesTextSize,
        color = textColor,
        shadow = null,
        ignoreAscentDescent = true
      )
      drawTextCentred(
        textMeasurer,
        coordinatesX[i],
        getCellCenter(i, 0, measurements).x,
        measurements.cellSize * boardHeight + measurements.border / 2,
        textSize = measurements.coordinatesTextSize,
        color = textColor,
        shadow = null,
        ignoreAscentDescent = true
      )
    }
    for (i in boardHeight downTo 1) {
      drawTextCentred(
        textMeasurer,
        coordinatesY[i - 1],
        0f - measurements.border / 2,
        getCellCenter(0, boardHeight - i, measurements).y,
        textSize = measurements.coordinatesTextSize,
        color = textColor,
        shadow = null
      )
      drawTextCentred(
        textMeasurer,
        coordinatesY[i - 1],
        measurements.cellSize * boardWidth + measurements.border / 2,
        getCellCenter(0, boardHeight - i, measurements).y,
        textSize = measurements.coordinatesTextSize,
        color = textColor,
        shadow = null
      )
    }
  }

}

internal fun doMeasurements(
  width: Int,
  height: Int,
  boardWidth: Int,
  boardHeight: Int,
  drawCoordinates: Boolean,
): Measurements {
  val usableWidth = if (drawCoordinates) {
    width - (width / boardWidth.toFloat()).roundToInt()
  } else width

  val usableHeight = if (drawCoordinates) {
    height - (height / boardHeight.toFloat()).roundToInt()
  } else height

  val cellSize = minOf(usableWidth / boardWidth, usableHeight / boardHeight)
  val border = minOf((width - boardWidth * cellSize) / 2f, (width - boardHeight * cellSize) / 2f)
  val xOffsetForNonSquareBoard = ((boardHeight - boardWidth) * cellSize / 2f).coerceAtLeast(0f)
  val yOffsetForNonSquareBoard = ((boardWidth - boardHeight) * cellSize / 2f).coerceAtLeast(0f)

  val linesWidth = (cellSize / 35f).coerceAtMost(2f)
  val highlightLinesWidth = linesWidth * 2
  val decorationsLineWidth = cellSize / 20f
  val stoneSpacing = (cellSize / 35f).coerceAtLeast(1f)
  val textSize = cellSize.toFloat() * .65f
  val coordinatesTextSize = cellSize.toFloat() * .4f
  val halfCell = cellSize / 2f
  val stoneRadius = (cellSize / 2f - stoneSpacing).toInt()
  val shadowSigma = cellSize / 11f * 0.57735f + 0.5f
  val shadowCoreRadius = stoneRadius - 2.5f
  val stoneShadowRadius = (shadowCoreRadius + 3 * shadowSigma).coerceAtLeast(1f)
  val stoneShadow = Brush.radialGradient(
    *(-4..4).map { step ->
      val radius = shadowCoreRadius + step * .75f * shadowSigma
      (radius / stoneShadowRadius).coerceIn(0f, 1f) to
          Color.Black.copy(alpha = SHADOW_ALPHA / (1 + exp(1.702f * .75f * step)))
    }.toTypedArray(),
    center = Offset.Zero,
    radius = stoneShadowRadius,
  )

  val centers = ArrayList<List<Offset>>(boardWidth)
  for (i in 0 until boardWidth) {
    val row = ArrayList<Offset>(boardHeight)
    centers.add(row)
    for (j in 0 until boardHeight) {
      row.add(Offset(i * cellSize + halfCell, j * cellSize + halfCell))
    }
  }

  return Measurements(
    cellSize = cellSize,
    border = border,
    linesWidth = linesWidth,
    highlightLinesWidth = highlightLinesWidth,
    decorationsLineWidth = decorationsLineWidth,
    stoneSpacing = stoneSpacing,
    width = width,
    textSize = textSize,
    coordinatesTextSize = coordinatesTextSize,
    stoneShadow = stoneShadow,
    stoneShadowRadius = stoneShadowRadius,
    stoneShadowOffset = stoneSpacing * 2 - 1,
    halfCell = halfCell,
    stoneRadius = stoneRadius,
    xOffsetForNonSquareBoard = xOffsetForNonSquareBoard,
    yOffsetForNonSquareBoard = yOffsetForNonSquareBoard,
    centers = centers,
  )
}
