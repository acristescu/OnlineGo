package io.zenandroid.onlinego.data.model

import android.graphics.Point
import android.util.Log
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
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

    val whiteStones: Set<Point>
        get() = stones.filter { it.value == StoneType.WHITE }.keys
    val blackStones: Set<Point>
        get() = stones.filter { it.value == StoneType.BLACK }.keys

    val whiteDeadStones: Collection<Point>
        get() = removedSpots.filter { stones[it] == StoneType.WHITE }
    val blackDeadStones: Collection<Point>
        get() = removedSpots.filter { stones[it] == StoneType.BLACK }

    val lastPlayerToMove: StoneType?
        get() = lastMove?.let { getStoneAt(it.x, it.y) }
    var variation: List<Point> = listOf()

    var parentPosition: Position? = null

    val customMarks = HashSet<Mark>()

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
        newPos.nextToMove = nextToMove
        newPos.parentPosition = parentPosition
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Position

        if (boardSize != other.boardSize) return false
        if (stones != other.stones) return false
        if (removedSpots != other.removedSpots) return false
        if (whiteTerritory != other.whiteTerritory) return false
        if (blackTerritory != other.blackTerritory) return false
        if (lastMove != other.lastMove) return false
        if (whiteCapturedCount != other.whiteCapturedCount) return false
        if (blackCapturedCount != other.blackCapturedCount) return false
        if (nextToMove != other.nextToMove) return false

        return true
    }

    fun hasTheSameStonesAs(other: Position) =
            stones == other.stones

    override fun hashCode(): Int {
        var result = boardSize
        result = 31 * result + stones.hashCode()
        result = 31 * result + removedSpots.hashCode()
        result = 31 * result + whiteTerritory.hashCode()
        result = 31 * result + blackTerritory.hashCode()
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        result = 31 * result + whiteCapturedCount
        result = 31 * result + blackCapturedCount
        result = 31 * result + nextToMove.hashCode()
        return result
    }

    data class Mark(
            val placement: Point,
            val text: String?,
            val category: PlayCategory?
    )

    companion object {
        fun fromJosekiPosition(josekiPosition: JosekiPosition): Position {
            var pos = Position(19)
            josekiPosition.play?.let {
                val moves = when {
                    it.startsWith(".root") -> it.substring(5)
                    it.startsWith(".root.") -> it.substring(6)
                    else -> it
                }.split('.').filter { it != "" }

                var turn  = StoneType.BLACK
                moves.forEach {
                    if(it != "pass") {
                        val point = coordinateToPoint(it)
                        val newPos = RulesManager.makeMove(pos, turn, point)
                        if(newPos == null) {
                            Log.e("Position", "Invalid joseki move!!!")
                            return pos
                        }
                        pos = newPos
                    }
                    turn = turn.opponent
                }
                pos.nextToMove = turn
            }
            josekiPosition.next_moves
                    ?.filter { it.placement != null && it.placement != "pass" && it.placement != "root"}
                    ?.map {
                        val childCoordinate = coordinateToPoint(it.placement!!)
                        val overlayLabel = josekiPosition.labels?.find { childCoordinate == it.placement }
                        if(overlayLabel == null) {
                            Mark(
                                    childCoordinate,
                                    it.variation_label,
                                    it.category
                            )
                        } else {
                            Mark(
                                    childCoordinate,
                                    overlayLabel.text,
                                    it.category
                            )
                        }
                    }
                    ?.let (pos.customMarks::addAll)
            josekiPosition.labels
                    ?.filter { candidate ->
                        josekiPosition.next_moves
                                ?.find { it.placement != null && it.placement != "pass" && candidate.placement == coordinateToPoint(it.placement!!) } == null
                    }
                    ?.map { Mark(it.placement, it.text, it.category) }
                    ?.let (pos.customMarks::addAll)
            return pos
        }

        private val coordinatesX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")

        fun coordinateToPoint(coordinate: String) : Point =
                Point(
                        coordinatesX.indexOf(coordinate.substring(0, 1)),
                        (19 - coordinate.substring(1).toInt())
                )
    }

}
