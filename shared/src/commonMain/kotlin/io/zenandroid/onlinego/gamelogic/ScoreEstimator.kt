package io.zenandroid.onlinego.gamelogic

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType

object ScoreEstimator {

  fun determineTerritory(pos: Position, scoreStones: Boolean): Position {
    val inBoard = IntArray(pos.boardWidth * pos.boardHeight)
    pos.blackStones
      .filter { !pos.removedSpots.contains(it) }
      .forEach {
        inBoard[it.x * pos.boardHeight + it.y] = 1
      }
    pos.whiteStones
      .filter { !pos.removedSpots.contains(it) }
      .forEach {
        inBoard[it.x * pos.boardHeight + it.y] = -1
      }
    val outBoard = NativeEstimator.estimate(
      pos.boardHeight, // Note: There is a bug in the estimator somewhere, width and height should be in the different order!!!
      pos.boardWidth, // Note: There is a bug in the estimator somewhere, width and height should be in the different order!!!
      inBoard,
      if (pos.nextToMove == StoneType.BLACK) 1 else -1,
      1000,
      .3f
    )
    val whiteTerritory = mutableSetOf<Cell>()
    val blackTerritory = mutableSetOf<Cell>()
    val removedCells = mutableSetOf<Cell>()

    for (x in 0 until pos.boardWidth) {
      for (y in 0 until pos.boardHeight) {
        when (outBoard[x * pos.boardHeight + y]) {
          -1 -> {
            val cell = Cell(x, y)
            whiteTerritory += cell
            if (pos.getStoneAt(cell) == StoneType.BLACK) {
              removedCells += cell
            }
          }

          0 -> {
            removedCells += Cell(x, y)
          }

          1 -> {
            val cell = Cell(x, y)
            blackTerritory += cell
            if (pos.getStoneAt(cell) == StoneType.WHITE) {
              removedCells += cell
            }
          }
        }
      }
    }

    if (!scoreStones) {
      whiteTerritory.removeAll(pos.whiteStones)
      blackTerritory.removeAll(pos.blackStones)
    }
    return pos.copy(
      whiteTerritory = whiteTerritory,
      blackTerritory = blackTerritory,
      removedSpots = removedCells,
    )
  }
}
