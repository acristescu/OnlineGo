package io.zenandroid.onlinego.model

import android.graphics.Point
import java.util.*

/**
 * A class that models a GO position. It is basically a collection of stones
 * that are placed on the intersections of the lines of a GO board.
 * For quick retrieval, the stones are kept as a HashMap (a hashtable).
 *
 * To determine if a stone is present at a position (and which kind) a user
 * can use the getStoneAt() method.
 *
 * To morph the position one can use the makeMove() method. This method is responsible
 * for validating the move and changing the Position to the new one by enforcing
 * captures for example.
 *
 * Lastly, the user can use getAllStonesCoordinates() to loop throught all the stones.
 *
 * Created by alex on 1/8/2015.
 */
class Position(val boardSize: Int) {

    private val stones = HashMap<Point, StoneType>()

    val removedSpots = HashSet<Point>()
    val whiteTerritory = HashSet<Point>()
    val blackTerritory = HashSet<Point>()

    var lastMove: Point? = null
    var whiteCapturedCount = 0
    var blackCapturedCount = 0

    var nextToMove = StoneType.BLACK

    val allStonesCoordinates: Set<Point>
        get() = stones.keys

    val lastPlayerToMove: StoneType?
        get() = lastMove?.let { getStoneAt(it.x, it.y) }

    /**
     * Adds a stone without checking the game logic. See makeMove() for alternative
     * @param i
     * @param j
     * @param type
     */
    fun putStone(i: Int, j: Int, type: StoneType) {
        stones[Point(i, j)] = type
    }

    /**
     * Returns the type of stone at the specified intersection or null if there is
     * no stone there.
     * @param i
     * @param j
     * @return
     */
    fun getStoneAt(i: Int, j: Int): StoneType? {
        return stones[Point(i, j)]
    }

    fun getStoneAt(p: Point?): StoneType? {
        return stones[p]
    }

    fun clone(): Position {
        val newPos = Position(boardSize)
        newPos.stones.putAll(stones)
        newPos.lastMove = lastMove
        newPos.blackCapturedCount = blackCapturedCount
        newPos.whiteCapturedCount = whiteCapturedCount
        newPos.blackTerritory.addAll(blackTerritory)
        newPos.whiteTerritory.addAll(whiteTerritory)
        newPos.removedSpots.addAll(removedSpots)
        return newPos
    }

    fun removeStone(p: Point) {
        stones.remove(p)
    }

    fun markRemoved(point: Point) {
        removedSpots.add(point)
    }

    fun clearAllRemovedSpots() {
        removedSpots.clear()
    }

    fun clearAllMarkedTerritory() {
        whiteTerritory.clear()
        blackTerritory.clear()
    }

    fun markWhiteTerritory(point: Point) {
        whiteTerritory.add(point)
    }

    fun markBlackTerritory(point: Point) {
        blackTerritory.add(point)
    }

}
