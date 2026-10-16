package io.zenandroid.onlinego.gamelogic

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreEstimatorTest {

  @Test
  fun splitsNineByNineBoardAlongTheWalls() = assertWallsSplitBoard(9)

  @Test
  fun splitsNineteenByNineteenBoardOnBackgroundThread() = assertWallsSplitBoard(19)

  private fun assertWallsSplitBoard(size: Int) = runTest {
    val blackWall = size / 2 - 1
    val whiteWall = size / 2 + 1
    val position = Position(
      boardWidth = size,
      boardHeight = size,
      blackStones = (0 until size).map { Cell(blackWall, it) }.toSet(),
      whiteStones = (0 until size).map { Cell(whiteWall, it) }.toSet(),
    )

    val estimate = withContext(Dispatchers.Default) {
      ScoreEstimator.determineTerritory(position, scoreStones = false)
    }

    val expectedBlack =
      (0 until blackWall).flatMap { x -> (0 until size).map { Cell(x, it) } }.toSet()
    val expectedWhite =
      (whiteWall + 1 until size).flatMap { x -> (0 until size).map { Cell(x, it) } }.toSet()
    assertEquals(expectedBlack, estimate.blackTerritory)
    assertEquals(expectedWhite, estimate.whiteTerritory)
    assertTrue(estimate.blackDeadStones.isEmpty())
    assertTrue(estimate.whiteDeadStones.isEmpty())
  }
}
