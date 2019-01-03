package io.zenandroid.onlinego.gamelogic

import android.graphics.Point
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import java.util.*

/**
 * Created by alex on 1/9/2015.
 */
object Util {

    fun getSGFCoordinates(p: Point): String {
        if (p.x == -1) {
            return ".."
        }
        val column = 'a' + p.x
        val row = 'a' + p.y
        return String(charArrayOf(column, row))
    }

    fun getCoordinatesFromSGF(sgf: String, offset: Int): Point {
        val column = sgf[offset]
        val row = sgf[offset + 1]

        val point = Point()
        point.x = column - 'a'
        point.y = row - 'a'

        return point
    }

    fun getNeighbouringSpace(current: Point, boardSize: Int): List<Point> {
        val left = Point(current)
        left.offset(-1, 0)
        val right = Point(current)
        right.offset(1, 0)
        val up = Point(current)
        up.offset(0, -1)
        val down = Point(current)
        down.offset(0, 1)

        val list = LinkedList<Point>()
        if (up.x in 0..(boardSize - 1) && up.y in 0..(boardSize - 1)) {
            list.add(up)
        }
        if (down.x in 0..(boardSize - 1) && down.y in 0..(boardSize - 1)) {
            list.add(down)
        }
        if (left.x in 0..(boardSize - 1) && left.y in 0..(boardSize - 1)) {
            list.add(left)
        }
        if (right.x in 0..(boardSize - 1) && right.y in 0..(boardSize - 1)) {
            list.add(right)
        }

        return list
    }

    fun isMyTurn(game: OGSGame?): Boolean {
        if (game?.player_to_move != 0L) {
            return game?.player_to_move == OGSServiceImpl.uiConfig?.user?.id
        }
        game.json?.let {
            return it.clock.current_player == OGSServiceImpl.uiConfig?.user?.id
        }
        return false
    }

    fun isMyTurn(game: Game?): Boolean {
        if (game?.playerToMoveId != null) {
            return game.playerToMoveId == OGSServiceImpl.uiConfig?.user?.id
        }
        return false
    }
}
