package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.theme.LocalPreloadedImages
import io.zenandroid.onlinego.ui.theme.loadPreloadedImages
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BoardTest {

  private val position = Position(
    boardWidth = 9,
    boardHeight = 9,
    blackStones = setOf(Cell(2, 2)),
    whiteStones = setOf(Cell(6, 6)),
    lastMove = Cell(6, 6),
    lastPlayerToMove = StoneType.WHITE,
    variation = listOf(Cell(4, 4)),
    customMarks = setOf(Mark(Cell(1, 7), "A")),
  )

  @Test
  fun drawsStonesFromTheResourcesAndReportsTaps() = runComposeUiTest {
    val images = runBlocking { loadPreloadedImages(BoardTheme.WOOD, false, Density(2f)) }
    val taps = mutableListOf<Cell>()
    setContent {
      CompositionLocalProvider(LocalPreloadedImages provides images) {
        Board(
          modifier = Modifier.size(270.dp).testTag("board"),
          boardWidth = 9,
          boardHeight = 9,
          position = position,
          boardTheme = BoardTheme.WOOD,
          drawCoordinates = false,
          fadeInLastMove = false,
          onTapUp = { taps += it },
        )
      }
    }

    val pixels = onNodeWithTag("board").captureToImage().toPixelMap()
    val cellSize = pixels.width / 9
    val border = (pixels.width - 9 * cellSize) / 2
    fun center(cell: Cell) = Offset(
      border + (cell.x + .5f) * cellSize,
      border + (cell.y + .5f) * cellSize,
    )

    fun luminance(cell: Cell) =
      center(cell).let { pixels[it.x.toInt(), it.y.toInt() + cellSize / 8] }
        .run { (red + green + blue) / 3 }

    assertTrue(luminance(Cell(2, 2)) < .3f, "black stone: ${luminance(Cell(2, 2))}")
    assertTrue(luminance(Cell(6, 6)) > .7f, "white stone: ${luminance(Cell(6, 6))}")

    onNodeWithTag("board").performTouchInput {
      down(center(Cell(3, 5)))
      moveTo(center(Cell(4, 5)))
      up()
    }
    assertEquals(listOf(Cell(4, 5)), taps)
  }

  @Test
  fun centresTheInkOfLabelsOnTheirPoint() = runComposeUiTest {
    val labels = listOf(Cell(1, 1), Cell(4, 1), Cell(7, 1), Cell(1, 4), Cell(4, 4), Cell(7, 4))
    setContent {
      Board(
        modifier = Modifier.size(450.dp).testTag("board"),
        boardWidth = 9,
        boardHeight = 9,
        position = Position(
          boardWidth = 9,
          boardHeight = 9,
          variation = labels,
          customMarks = setOf(Mark(Cell(4, 7), "Q")),
        ),
        boardTheme = BoardTheme.BOOK,
        drawCoordinates = false,
      )
    }

    val pixels = onNodeWithTag("board").captureToImage().toPixelMap()
    val cellSize = pixels.width / 9
    val border = (pixels.width - 9 * cellSize) / 2
    (labels + Cell(4, 7)).forEachIndexed { index, cell ->
      val cx = border + (cell.x + .5f) * cellSize
      val cy = border + (cell.y + .5f) * cellSize
      val ink = (cx.toInt() - cellSize / 2 until cx.toInt() + cellSize / 2).flatMap { x ->
        (cy.toInt() - cellSize / 2 until cy.toInt() + cellSize / 2).map { y -> x to y }
      }.filter { (x, y) -> pixels[x, y].run { (red + green + blue) / 3 } < .25f }
      val inkX = (ink.minOf { it.first } + ink.maxOf { it.first } + 1) / 2f
      val inkY = (ink.minOf { it.second } + ink.maxOf { it.second } + 1) / 2f
      assertTrue(
        abs(inkX - cx) <= 1.5f && abs(inkY - cy) <= 1.5f,
        "label ${index + 1} ink centre ($inkX, $inkY), point ($cx, $cy)",
      )
    }
  }
}
