package io.zenandroid.onlinego.gamelogic

import android.graphics.Point
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import java.util.*

/**
 * Created by alex on 14/11/2017.
 */
object RulesManager {

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