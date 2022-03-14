package io.zenandroid.onlinego.gamelogic

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import okhttp3.internal.toImmutableList
import org.koin.core.context.GlobalContext.get
import kotlin.math.min
import kotlin.math.max
import java.util.*

/**
 * Created by alex on 1/9/2015.
 */
object Util {

    private val userSessionRepository: UserSessionRepository by get().inject()
    private val coordinatesX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")
    private val coordinatesY = (1..25).map(Int::toString)

    fun getGTPCoordinates(p: Cell, boardHeight: Int): String {
        if (p.x == -1) {
            return "PASS"
        }

        return "${coordinatesX[p.x]}${coordinatesY[boardHeight - p.y - 1]}"
    }

    fun getCoordinatesFromGTP(gtp: String, boardHeight: Int): Cell {
        if(gtp.toUpperCase(Locale.ROOT) == "PASS") {
            return Cell(-1, -1)
        }

        val x = coordinatesX.indexOf(gtp.substring(0..0))
        val y = boardHeight - gtp.substring(1).toInt()
        return Cell(x, y)
    }

    fun getSGFCoordinates(p: Cell): String {
        if (p.x == -1) {
            return ".."
        }
        val column = 'a' + p.x
        val row = 'a' + p.y
        return String(charArrayOf(column, row))
    }

    fun getCoordinatesFromSGF(sgf: String, offset: Int = 0): Cell {
        val column = sgf[offset]
        val row = sgf[offset + 1]

        return Cell(column - 'a', row - 'a')
    }

    fun Position.populateWithSGF(sgf: String) {
        for(i in sgf.indices step 5) {
            val p = getCoordinatesFromSGF(sgf, i + 2)
            val stone = if(sgf[i] == 'B') StoneType.BLACK else StoneType.WHITE
            putStone(p.x, p.y, stone)
        }
    }

    fun Position.populateWithMarks(marks: String) {
        for(i in marks.indices step 5) {
            val p = getCoordinatesFromSGF(marks, i + 2)
            customMarks.add(Position.Mark(p, marks[i].toString(), null))
        }
    }

    fun Position.populateWithAreas(areas: String) {
        for(i in areas.indices step 7) {
            val corner1 = getCoordinatesFromSGF(areas, i+2)
            val corner2 = getCoordinatesFromSGF(areas, i+4)
            for(x in min(corner1.x, corner2.x).. max(corner1.x, corner2.x)) {
                for(y in min(corner1.y, corner2.y) .. max(corner1.y, corner2.y)) {
                    customMarks.add(Position.Mark(Cell(x, y), areas[i].toString(), null))
                }
            }
        }
    }

    fun sgfToPositionList(sgf: String, size: Int): List<Position> {
        var pos = Position(size, size)
        val retval = mutableListOf<Position>()
        retval.add(pos)
        for(i in sgf.indices step 5) {
            val p = getCoordinatesFromSGF(sgf, i + 2)
            val stone = if(sgf[i] == 'B') StoneType.BLACK else StoneType.WHITE
            pos = RulesManager.makeMove(pos, stone, p)!!
            retval.add(pos)
        }
        return retval.toImmutableList()
    }

    fun getNeighbouringSpace(current: Cell, boardWidth: Int, boardHeight: Int): List<Cell> {
        val left = current.leftNeighbour
        val right = current.rightNeighbour
        val up = current.topNeighbour
        val down = current.bottomNeighbour

        val list = LinkedList<Cell>()
        if (up.x in 0 until boardWidth && up.y in 0 until boardHeight) {
            list.add(up)
        }
        if (down.x in 0 until boardWidth && down.y in 0 until boardHeight) {
            list.add(down)
        }
        if (left.x in 0 until boardWidth && left.y in 0 until boardHeight) {
            list.add(left)
        }
        if (right.x in 0 until boardWidth && right.y in 0 until boardHeight) {
            list.add(right)
        }

        return list
    }

    fun isMyTurn(game: OGSGame?): Boolean {
        if (game?.player_to_move != 0L) {
            return game?.player_to_move == getCurrentUserId()
        }
        game.json?.let {
            return it.clock?.current_player == getCurrentUserId()
        }
        return false
    }

    fun isMyTurn(game: Game?): Boolean {
        if (game?.playerToMoveId != null) {
            return game.playerToMoveId == getCurrentUserId()
        }
        return false
    }

    fun getCurrentUserId() =
        userSessionRepository.userId

    fun getRemovedStonesInLastMove(position: Position): Map<Cell, StoneType> =
        position.parentPosition?.stones?.filter { !position.stones.contains(it.key)} ?: emptyMap()
}
