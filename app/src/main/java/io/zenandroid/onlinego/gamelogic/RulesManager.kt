package io.zenandroid.onlinego.gamelogic

import android.graphics.Point
import android.util.Log
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.ogs.GameData
import java.util.*

/**
 * Created by alex on 14/11/2017.
 */
object RulesManager {

    init {
        println("loading library")
        System.loadLibrary("estimator")
    }

    private external fun estimate(w: Int, h: Int, board: IntArray, playerToMove: Int, trials: Int, tolerance: Float): IntArray

    fun determineTerritory(pos: Position) {
        val inBoard = IntArray(pos.boardSize * pos.boardSize)
        pos.allStonesCoordinates
                .filter { !pos.removedSpots.contains(it) }
                .forEach {
                    val type = pos.getStoneAt(it)
                    inBoard[it.x * pos.boardSize + it.y] = if(type == StoneType.BLACK) 1 else -1
                }
        val outBoard = estimate(
                pos.boardSize,
                pos.boardSize,
                inBoard,
                if(pos.lastPlayerToMove.opponent == StoneType.BLACK) 1 else -1,
                10000,
                .3f)
        pos.clearAllMarkedTerritory()
        pos.clearAllRemovedSpots()
        for(x in 0 until pos.boardSize) {
            for(y in 0 until pos.boardSize) {
                when(outBoard[x * pos.boardSize + y]) {
                    -1 -> {
                        pos.markWhiteTerritory(Point(x, y))
                        if(pos.getStoneAt(x, y) == StoneType.BLACK) {
                            pos.markRemoved(Point(x, y))
                        }
                    }
                    0 -> {
                        pos.markRemoved(Point(x, y))
                    }
                    1 -> {
                        pos.markBlackTerritory(Point(x, y))
                        if(pos.getStoneAt(x, y) == StoneType.WHITE) {
                            pos.markRemoved(Point(x, y))
                        }
                    }
                }
            }
        }
    }

    fun replay(gameData: GameData, limit: Int = Int.MAX_VALUE): Position {
        var pos = Position(gameData.height)

        var turn = StoneType.BLACK
        if(gameData.initial_player == "white") {
            turn = StoneType.WHITE
        }

        gameData.moves.forEachIndexed { index, move ->
            if(index >= limit) {
                return@forEachIndexed
            }
            val newPos = RulesManager.makeMove(pos, turn, Point(move[0], move[1]))
            if(newPos == null) {
                Log.e(this.javaClass.simpleName, "Server returned an invalid move!!! gameId=${gameData.game_id} move=$index")
                return@forEachIndexed
            }
            pos = newPos
            if(index + 1 >= gameData.handicap ?: 0) {
                turn = turn.opponent
            }
        }
        gameData.removed?.let {
            for (i in 0 until it.length step 2) {
                pos.markRemoved(Util.getCoordinatesFromSGF(it, i))
            }
        }
        for(i in 0 until pos.boardSize) {
            (0 until pos.boardSize)
                    .map { Point(i, it) }
                    .filter { !isMarkedDame(pos, it) }
                    .filter { !isLivingStone(pos, it) }
                    .filter { !pos.whiteTerritory.contains(it) }
                    .filter { !pos.blackTerritory.contains(it) }
                    .forEach { markEye(pos, it) }
        }
        gameData.score?.white?.scoring_positions?.let {
            for (i in 0 until it.length step 2) {
                pos.markWhiteTerritory(Util.getCoordinatesFromSGF(it, i))
            }
        }
        gameData.score?.black?.scoring_positions?.let {
            for (i in 0 until it.length step 2) {
                pos.markBlackTerritory(Util.getCoordinatesFromSGF(it, i))
            }
        }
        return pos
    }

    private fun markEye(pos: Position, point: Point) {
        val toVisit = mutableListOf(point)
        val visited = mutableSetOf<Point>()
        var foundWhite = false
        var foundBlack = false
        while(!toVisit.isEmpty() && !(foundBlack && foundWhite)) {
            val p = toVisit.removeAt(toVisit.size - 1)
            visited.add(p)
            if(isLivingStone(pos, p)) {
                if(pos.getStoneAt(p) == StoneType.WHITE) {
                    foundWhite = true
                } else {
                    foundBlack = true
                }
                continue
            }
            if(isMarkedDame(pos, p)) {
                continue
            }
            toVisit.addAll(
                    Util.getNeighbouringSpace(p, pos.boardSize)
                            .filter { !visited.contains(it) })
        }
        if(foundWhite && !foundBlack) {
            visited
                    .filter { !isMarkedDame(pos, it) }
                    .filter { !isLivingStone(pos, it) }
                    .forEach { pos.markWhiteTerritory(it) }
        } else if(foundBlack && !foundWhite) {
            visited
                    .filter { !isMarkedDame(pos, it) }
                    .filter { !isLivingStone(pos, it) }
                    .forEach { pos.markBlackTerritory(it) }
        }
    }

    private fun isMarkedDame(pos: Position, p: Point) =
            pos.getStoneAt(p) == null && pos.removedSpots.contains(p)

    private fun isLivingStone(pos: Position, p: Point) =
            pos.getStoneAt(p) != null && !pos.removedSpots.contains(p)
    /**
     * Morph this postion to a new one by performing the move specified.
     * If the move is invalid, no action is taken and the method returns false.
     * If the move results in a capture, the captured group is removed from the
     * position.
     * @param stone
     * @param where
     * @return
     */
    fun makeMove(oldPos: Position, stone: StoneType, where: Point): Position? {
        val pos = oldPos.clone()
        if (where.x == -1) {
            //
            // it's a pass
            //
            return pos
        }
        if (pos.getStoneAt(where.x, where.y) != null) {
            //
            // Can't place a stone on top of another
            //
            return null
        }

        pos.putStone(where.x, where.y, stone)

        //
        // Determine if the newly placed stone results in a capture.
        // For this, we're calling doCapture() on all the neighbours
        // of the new stones that are of opposite color
        //
        val removedStones = LinkedList<Point>()
        val neighbours = Util.getNeighbouringSpace(where, pos.boardSize)

        for (neighbour in neighbours) {
            val neighbourType = pos.getStoneAt(neighbour)
            if (neighbourType != null && neighbourType != stone) {
                doCapture(pos, neighbour, neighbourType)?.let {
                    removedStones.addAll(it)
                }
            }
        }

        if (!removedStones.isEmpty()) {
            for (p in removedStones) {
                pos.removeStone(p)
            }
            when(stone) {
                StoneType.WHITE -> pos.whiteCapturedCount += removedStones.size
                StoneType.BLACK -> pos.blackCapturedCount += removedStones.size
            }
        } else {
            //
            // We need to check for suicide
            //
            val suicideGroup = doCapture(pos, where, stone)
            if (suicideGroup != null && !suicideGroup.isEmpty()) {
                pos.removeStone(where)
                return null
            }
        }

        pos.lastMove = where
        return pos
    }

    /**
     * Check if the stone group that contains the stone passed as a
     * parameter is completely surrounded by opposing pieces and the edges
     * of the board. If so, the entire group is returned
     *
     * @param origin
     * @param type
     * @return
     */
    private fun doCapture(pos: Position, origin: Point, type: StoneType): List<Point>? {
        //
        // For this, we're using a simplified shape recognition mechanism
        // For each visited node, we're getting all the neighbours, checking
        // to see if any of them is empty (in which case we immediately exit,
        // because it means we have no capture) and if not, we add the
        // neighbours of the same color with the original to the toVisit list
        // Lastly, we move the visited node from the toVisit to the visited list
        // At the end, if we did not return before the toVisit list is empty
        // it means the group is surrounded and the contents of the visited
        // list is returned.
        //
        val toVisit = LinkedList<Point>()
        val visited = LinkedList<Point>()

        toVisit.add(origin)

        while (!toVisit.isEmpty()) {
            val current = toVisit.pop()
            visited.add(current)
            val neighbours = Util.getNeighbouringSpace(current, pos.boardSize)

            for (toCheck in neighbours) {
                val checkedStoneType = pos.getStoneAt(toCheck) ?:
                        //
                        // A liberty, hence no capture
                        //
                        return null
                if (checkedStoneType == type && !visited.contains(toCheck)) {
                    toVisit.add(toCheck)
                }
            }
        }

        return visited
    }

}