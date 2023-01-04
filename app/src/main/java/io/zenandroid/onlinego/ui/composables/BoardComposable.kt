package io.zenandroid.onlinego.ui.composables

import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager.isPass
import org.koin.core.context.GlobalContext
import kotlin.math.ceil
import kotlin.math.roundToInt


@ExperimentalComposeUiApi
@Composable
fun Board(
    modifier: Modifier = Modifier,
    boardWidth: Int,
    boardHeight: Int,
    position: Position?,
    candidateMove: Cell? = null,
    candidateMoveType: StoneType? = null,
    boardTheme: BoardTheme,
    drawCoordinates: Boolean = true,
    interactive: Boolean = true,
    drawShadow: Boolean = true,
    drawTerritory: Boolean = false,
    drawLastMove: Boolean = true,
    lastMoveMarker: String = "#",
    fadeInLastMove: Boolean = true,
    fadeOutRemovedStones: Boolean = true,
    removedStones: List<Pair<Cell, StoneType>>? = null,
    onTapMove: ((Cell) -> Unit)? = null,
    onTapUp: ((Cell) -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {

        val drawMarks = true

        // Board background image, it is either a jpg or a svg
        val backgroundImage: ImageBitmap? = boardTheme.backgroundImage?.let {
            ImageBitmap.imageResource(id = boardTheme.backgroundImage)
        }
        val backgroundColor: Color? = boardTheme.backgroundColor?.let {
            colorResource(boardTheme.backgroundColor)
        }

        // Stones images
        val blackStone = rememberVectorPainter(image = ImageVector.vectorResource(id = boardTheme.blackStone))
        val whiteStone = rememberVectorPainter(image = ImageVector.vectorResource(id = boardTheme.whiteStone))

        val width = with(LocalDensity.current) { maxWidth.roundToPx() }
        val height = with(LocalDensity.current) { maxHeight.roundToPx() }

        val measurements = remember(width, height, boardWidth, boardHeight) { doMeasurements(width, height, boardWidth, boardHeight, drawCoordinates) }

        var lastHotTrackedPoint: Cell? by remember { mutableStateOf(null) }
        var stoneToFadeIn: Cell? by remember(position?.lastMove) { mutableStateOf(position?.lastMove) }
        var stonesToFadeOut: List<Pair<Cell, StoneType>>? by remember(removedStones) { mutableStateOf(removedStones) }

        val fadeInAlpha = remember { Animatable(.4f) }
        val fadeOutAlpha = remember { Animatable(1f) }

        if(fadeInLastMove && stoneToFadeIn != null) {
            LaunchedEffect(stoneToFadeIn) {
                fadeInAlpha.snapTo(.4f)
                fadeInAlpha.animateTo(1f, animationSpec = tween(150))
                stoneToFadeIn = null
            }
        }
        if(fadeOutRemovedStones && stonesToFadeOut?.isNotEmpty() == true) {
            LaunchedEffect(stonesToFadeOut) {
                fadeOutAlpha.snapTo(1f)
                fadeOutAlpha.animateTo(0f, animationSpec = tween(150, 75))
                stonesToFadeOut = null
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()
            .run {
                if (interactive) {
                    pointerInteropFilter {
                        if (measurements.cellSize == 0) {
                            return@pointerInteropFilter false
                        }
                        val eventCoords =
                            screenToBoardCoordinates(it.x, it.y, measurements, boardWidth, boardHeight)

                        if (eventCoords != lastHotTrackedPoint) {
                            onTapMove?.invoke(eventCoords)
                            lastHotTrackedPoint = eventCoords
                        }

                        if (it.action == MotionEvent.ACTION_UP) {
                            onTapUp?.invoke(eventCoords)
                            lastHotTrackedPoint = null
                        }
                        true
                    }
                } else {
                    this
                }
            }
        ) {
            drawBackground(backgroundImage, backgroundColor)
            translate(measurements.border + measurements.xOffsetForNonSquareBoard, measurements.border + measurements.yOffsetForNonSquareBoard) {

                drawGrid(boardWidth, boardHeight, candidateMove, boardTheme.textAndGridColor, measurements)
                drawStarPoints(boardWidth, boardHeight, boardTheme.textAndGridColor, measurements)
                drawCoordinates(boardWidth, boardHeight, boardTheme, drawCoordinates, measurements)

                for (item in stonesToFadeOut ?: emptyList()) {
                    drawStone(item.first, item.second, if (item.second == StoneType.WHITE) whiteStone else blackStone, fadeOutAlpha.value, true, measurements)
                }
                position?.let {
                    // stones
                    for (p in position.whiteStones) {
                        val alpha = when {
                            fadeInLastMove && p == stoneToFadeIn -> fadeInAlpha.value
                            fadeOutRemovedStones && it.removedSpots.contains(p) -> .4f
                            else -> 1f
                        }
                        drawStone(p, StoneType.WHITE, whiteStone, alpha, drawShadow, measurements)
                    }
                    for (p in position.blackStones) {
                        val alpha = when {
                            fadeInLastMove && p == stoneToFadeIn -> fadeInAlpha.value
                            fadeOutRemovedStones && it.removedSpots.contains(p) -> .4f
                            else -> 1f
                        }
                        drawStone(p, StoneType.BLACK, blackStone, alpha, drawShadow, measurements)
                    }

                    drawDecorations(it, drawLastMove, drawMarks, lastMoveMarker, measurements)

                    if(drawTerritory) {
                        drawTerritory(it, measurements)
                    }

//                drawAiEstimatedOwnership(canvas, it)
//                drawHints(canvas, it)
                }
                candidateMove?.let {
                    drawStone(it, candidateMoveType, if (candidateMoveType == StoneType.WHITE) whiteStone else blackStone, .4f, false, measurements)
                }
            }
        }
    }
}

private fun screenToBoardCoordinates(x: Float, y: Float, measurements: Measurements, boardWidth: Int, boardHeight: Int): Cell {
    return Cell(
            ((x - measurements.border - measurements.xOffsetForNonSquareBoard).toInt() / measurements.cellSize).coerceIn(0, boardWidth - 1),
            ((y - measurements.border - measurements.yOffsetForNonSquareBoard).toInt() / measurements.cellSize).coerceIn(0, boardHeight - 1)
    )
}

private val coordinatesX = arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
private val coordinatesY = (1..25).map(Int::toString)

private data class Measurements(
    var width: Int,
    val cellSize: Int,
    val border: Float,
    val linesWidth: Float,
    val highlightLinesWidth: Float,
    val decorationsLineWidth: Float,
    val stoneSpacing: Float,
    val textSize: Float,
    val coordinatesTextSize: Float,
    val shadowPaint: Paint,
    val halfCell: Float,
    val stoneRadius: Int,
    val xOffsetForNonSquareBoard: Float,
    val yOffsetForNonSquareBoard: Float,
)

private fun getCellCenter(i: Int, j: Int, measurements: Measurements) =
        Offset(i * measurements.cellSize + measurements.halfCell, j * measurements.cellSize + measurements.halfCell)

private fun DrawScope.drawSingleStarPoint(i: Int, j: Int, color: Color, measurements: Measurements) {
    val center = getCellCenter(i, j, measurements)
    drawCircle(color, measurements.cellSize / 15f, Offset(center.x, center.y))
}

private fun DrawScope.drawTextCentred(text: String, cx: Float, cy: Float, textSize: Float, @ColorInt color: Int = android.graphics.Color.BLACK, shadow: Boolean = true, ignoreAscentDescent: Boolean = false) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val textBounds = Rect()
    paint.apply {
        textAlign = android.graphics.Paint.Align.LEFT
        isSubpixelText = true
        setTextSize(textSize)
        getTextBounds(text, 0, text.length, textBounds)
        setColor(color)
        if(shadow) {
            setShadowLayer(8.dp.value, 0f, 0f, android.graphics.Color.WHITE)
        }
    }

    drawIntoCanvas {
        it.nativeCanvas.drawText(text,
                cx - textBounds.exactCenterX(),
                cy - textBounds.exactCenterY() + if (ignoreAscentDescent) (textBounds.bottom.toFloat() / 2) else 0f,
                paint)
    }
}

private fun DrawScope.drawBackground(backgroundImage: ImageBitmap?, backgroundColor: Color?) {
    if (backgroundImage != null) {
        drawImage(image = backgroundImage, dstSize = IntSize(ceil(this.size.width).toInt(), ceil(this.size.height).toInt()))
    }
    if (backgroundColor != null) {
        val size = Size(ceil(this.size.width), ceil(this.size.height))
        drawRect(color = backgroundColor, size = size)
    }
}

private fun DrawScope.drawTerritory(position: Position, measurements: Measurements) {
    for(i in 0 until position.boardWidth) {
        for(j in 0 until position.boardHeight) {
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

private fun DrawScope.drawDecorations(position: Position, drawLastMove: Boolean, drawMarks: Boolean, lastMoveMarker: String, measurements: Measurements) {
    if(drawLastMove) {
        position.lastMove?.let {
            if(!it.isPass()) {
                val color = if (position.lastPlayerToMove == StoneType.WHITE) Color.Black else Color.White
                if(lastMoveMarker == "#") {
                    drawDefaultLastMoveMarker(it, measurements, color)
                } else {
                    val center = getCellCenter(it.x, it.y, measurements)
                    drawTextCentred(lastMoveMarker, center.x, center.y, textSize = measurements.textSize, color = color.toArgb())
                }
            }
        }
    }

    position.variation.forEachIndexed { i, p ->
        val center = getCellCenter(p.x, p.y, measurements)
        val stone = position.getStoneAt(p)
        val color = if(stone == null || stone == StoneType.WHITE) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        drawTextCentred((i + 1).toString(), center.x, center.y, textSize = measurements.textSize, color)
    }

    if(drawMarks) {
        position.customMarks.forEach {
            when(it.text) {
                "#" -> { // last move mark
                    val color = if (position.getStoneAt(it.placement) == StoneType.WHITE) Color.Black else Color.White
                    drawDefaultLastMoveMarker(it.placement, measurements, color)
                }
                else -> {
                    val center = getCellCenter(it.placement.x, it.placement.y, measurements)

                    val color = when(it.category) {
                        PlayCategory.IDEAL -> Color(0xC0D0FFC0)
                        PlayCategory.GOOD -> Color(0xC0F0FF90)
                        PlayCategory.MISTAKE -> Color(0xC0ED7861)
                        PlayCategory.TRICK -> Color(0xC0D384F9)
                        PlayCategory.QUESTION -> Color(0xC079B3E4)
                        PlayCategory.LABEL -> Color(0x70FFFFFF)
                        null -> Color(0x00FFFFFF)
                    }


                    drawCircle(color, measurements.cellSize / 2f - measurements.stoneSpacing, Offset(center.x, center.y))
                    it.text?.let {
                        drawTextCentred(it, center.x, center.y, textSize = measurements.textSize)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawDefaultLastMoveMarker(cell: Cell, measurements: Measurements, color: Color) {
    val center = getCellCenter(cell.x, cell.y, measurements)
    drawCircle(color, measurements.cellSize / 4f, center, style = Stroke(measurements.decorationsLineWidth))
}

private fun DrawScope.drawStone(p: Cell, type: StoneType?, stonePainter: Painter, alpha: Float = 1f, drawShadow: Boolean, measurements: Measurements) {
    val center = getCellCenter(p.x, p.y, measurements)
    drawIntoCanvas {
        if(drawShadow && alpha > .75) {
            if(Build.VERSION.SDK_INT >= 28) {
                it.drawCircle(Offset(center.x, center.y), measurements.stoneRadius - 2.5f, measurements.shadowPaint) // we don't care about the circle really, but the paint produces a fast shadow...
            } else {
                it.drawCircle(Offset(center.x + 2, center.y + 2), measurements.stoneRadius.toFloat(), measurements.shadowPaint)
            }
        }

        if(measurements.cellSize > 40) {
            translate (center.x - measurements.stoneRadius, center.y - measurements.stoneRadius) {
                with(stonePainter) {
                    draw(Size(measurements.stoneRadius * 2f, measurements.stoneRadius * 2f), alpha = alpha)
                }
            }
        } else {
            val color = if (type == StoneType.BLACK) Color.Black else Color.White

//            stonePaint.style = android.graphics.Paint.Style.FILL
            drawCircle(color = color, center = Offset(center.x, center.y), radius = measurements.stoneRadius.toFloat(), style = Fill)
            if(type == StoneType.WHITE) {
//                stonePaint.color = 0xCC331810.toInt()
//                stonePaint.style = android.graphics.Paint.Style.STROKE
                drawCircle(color = Color(0xCC331810), center = Offset(center.x, center.y), radius = measurements.stoneRadius.toFloat(), style = Stroke(1f))
            }

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

        val (color, lineWidth) = if(i == candidateMove?.x) Color.White to measurements.highlightLinesWidth else textAndGridColor to measurements.linesWidth
        drawLine(color, Offset(start, measurements.halfCell), Offset(start, fullLength - measurements.halfCell), lineWidth)
    }
    for (i in 0 until boardHeight) {
        val start = i * measurements.cellSize + measurements.halfCell
        val fullLength = (measurements.cellSize * boardWidth).toFloat()

        val (color, lineWidth) = if(i == candidateMove?.y) Color.White to measurements.highlightLinesWidth else textAndGridColor to measurements.linesWidth
        drawLine(color, Offset(measurements.halfCell, start), Offset(fullLength - measurements.halfCell, start), lineWidth)
    }
}

private fun DrawScope.drawStarPoints(boardWidth: Int, boardHeight: Int, color: Color, measurements: Measurements) {
    if(boardWidth == boardHeight) {
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

private fun DrawScope.drawCoordinates(boardWidth: Int, boardHeight: Int, boardTheme: BoardTheme, drawCoordinates: Boolean, measurements: Measurements) {
    if (drawCoordinates) {
        val textColor: Int = boardTheme.textAndGridColor.toArgb()
        for (i in 0 until boardWidth) {
            drawTextCentred(coordinatesX[i], getCellCenter(i, 0, measurements).x, 0f - measurements.border / 2, textSize = measurements.coordinatesTextSize, shadow = false, ignoreAscentDescent = true, color = textColor)
            drawTextCentred(coordinatesX[i], getCellCenter(i, 0, measurements).x, measurements.cellSize * boardHeight + measurements.border / 2, textSize = measurements.coordinatesTextSize, shadow = false, ignoreAscentDescent = true,  color = textColor)
        }
        for (i in boardHeight downTo 1) {
            drawTextCentred(coordinatesY[i - 1], 0f - measurements.border / 2, getCellCenter(0, boardHeight - i, measurements).y, textSize = measurements.coordinatesTextSize, shadow = false, color = textColor)
            drawTextCentred(coordinatesY[i - 1], measurements.cellSize * boardWidth + measurements.border / 2, getCellCenter(0, boardHeight - i, measurements).y, textSize = measurements.coordinatesTextSize, shadow = false, color = textColor)
        }
    }

}

private fun doMeasurements(
    width: Int,
    height: Int,
    boardWidth: Int,
    boardHeight: Int,
    drawCoordinates: Boolean,
): Measurements {
    val usableWidth = if (drawCoordinates) {
        width - (width  / boardWidth.toFloat()).roundToInt()
    } else width

    val usableHeight = if (drawCoordinates) {
        height - (height  / boardHeight.toFloat()).roundToInt()
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
    val shadowPaint = Paint().apply {
            asFrameworkPaint().run {
                color = if(Build.VERSION.SDK_INT >= 28) 0x01000000 else 0x22000000
                setShadowLayer(
                        cellSize / 11f,
                        0f,
                        stoneSpacing * 2 - 1,
                        0xBB000000.toInt()
                )
            }
        }
    val halfCell = cellSize / 2f
    val stoneRadius = (cellSize / 2f - stoneSpacing).toInt()

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
            shadowPaint = shadowPaint,
            halfCell = halfCell,
            stoneRadius = stoneRadius,
            xOffsetForNonSquareBoard = xOffsetForNonSquareBoard,
            yOffsetForNonSquareBoard = yOffsetForNonSquareBoard,
    )
}
