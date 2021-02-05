package io.zenandroid.onlinego.gamelogic

import android.graphics.Point
import android.util.Log
import androidx.core.util.lruCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.data.model.ogs.GameData
import java.util.*

/**
 * Created by alex on 14/11/2017.
 */
object RulesManager {

    init {
        println("loading library")
        System.loadLibrary("estimator")
    }

    private val positionsCache = lruCache<CacheKey, Position>(1000)

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
                if(pos.lastPlayerToMove?.opponent == StoneType.BLACK) 1 else -1,
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

    data class CacheKey(
            val width: Int,
            val height: Int,
            val initialState: InitialState?,
            val whiteGoesFirst: Boolean?,
            val moves: MutableList<MutableList<Int>>?,
            val freeHandicapPlacement: Boolean?,
            val handicap: Int?,
            val removedStones: String?,
            val white_scoring_positions: String?,
            val black_scoring_positions: String?,
            val computeTerritory: Boolean,
            val limit: Int
            )

    private fun getCacheKey(game: Game, limit: Int, computeTerritory: Boolean): CacheKey {
        return CacheKey(
                width = game.width,
                height = game.height,
                initialState = game.initialState,
                whiteGoesFirst = game.whiteGoesFirst,
                moves = game.moves,
                freeHandicapPlacement = game.freeHandicapPlacement,
                handicap = game.handicap,
                removedStones = game.removedStones,
                white_scoring_positions = game.whiteScore?.scoring_positions,
                black_scoring_positions = game.blackScore?.scoring_positions,
                computeTerritory = computeTerritory,
                limit = limit
        )
    }

    fun replay(game: Game, limit: Int = Int.MAX_VALUE, computeTerritory : Boolean): Position {
        val cacheKey = getCacheKey(game, limit, computeTerritory)
        positionsCache[cacheKey]?.let {
            return it
        }
        var pos = newPosition(game.height, game.initialState)

        var turn = StoneType.BLACK
        if(game.whiteGoesFirst == true) {
            turn = StoneType.WHITE
        }
        pos.nextToMove = turn

        game.moves?.forEachIndexed { index, move ->
            if(index >= limit) {
                return@forEachIndexed
            }
            val newPos = makeMove(pos, turn, Point(move[0], move[1]))
            if(newPos == null) {
                Log.e(this.javaClass.simpleName, "Server returned an invalid move!!! gameId=${game.id} move=$index")
                FirebaseCrashlytics.getInstance().log("E/RulesManager: Server returned an invalid move!!! gameId=${game.id} move=$index")
                return@forEachIndexed
            }
            pos = newPos
            val handicap = game.handicap ?: 0
            if(game.freeHandicapPlacement != true || index >= handicap - 1) {
                    turn = turn.opponent
            }
            newPos.nextToMove = turn
        }
        game.removedStones?.let {
            for (i in 0 until it.length step 2) {
                pos.markRemoved(Util.getCoordinatesFromSGF(it, i))
            }
        }
        //
        // WARNING: This is time consuming AF, avoid it like the plague
        //
        if(computeTerritory) {
            for (i in 0 until pos.boardSize) {
                (0 until pos.boardSize)
                        .map { Point(i, it) }
                        .filter { !isMarkedDame(pos, it) }
                        .filter { !isLivingStone(pos, it) }
                        .filter { !pos.whiteTerritory.contains(it) }
                        .filter { !pos.blackTerritory.contains(it) }
                        .forEach { markEye(pos, it) }
            }
        }

        game.whiteScore?.scoring_positions?.let {
            for (i in it.indices step 2) {
                pos.markWhiteTerritory(Util.getCoordinatesFromSGF(it, i))
            }
        }
        game.blackScore?.scoring_positions?.let {
            for (i in it.indices step 2) {
                pos.markBlackTerritory(Util.getCoordinatesFromSGF(it, i))
            }
        }

        positionsCache.put(cacheKey, pos)

        return pos
    }

    @Deprecated("Obsolete")
    fun replay(gameData: GameData, limit: Int = Int.MAX_VALUE, computeTerritory : Boolean): Position {
        var pos = RulesManager.newPosition(gameData.height, gameData.initial_state)

        var turn = StoneType.BLACK
        if(gameData.initial_player == "white") {
            turn = StoneType.WHITE
        }
        pos.nextToMove = turn

        gameData.moves.forEachIndexed { index, move ->
            if(index >= limit) {
                return@forEachIndexed
            }
            val newPos = RulesManager.makeMove(pos, turn, Point(move[0].toInt(), move[1].toInt()))
            if(newPos == null) {
                Log.e(this.javaClass.simpleName, "Server returned an invalid move!!! gameId=${gameData.game_id} move=$index")
                return@forEachIndexed
            }
            pos = newPos
            turn = turn.opponent
            newPos.nextToMove = turn
        }
        gameData.removed?.let {
            for (i in 0 until it.length step 2) {
                pos.markRemoved(Util.getCoordinatesFromSGF(it, i))
            }
        }
        //
        // WARNING: This is time consuming AF, avoid it like the plague
        //
        if(computeTerritory) {
            for (i in 0 until pos.boardSize) {
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
        }

        return pos
    }

    private fun newPosition(height: Int, initialState: InitialState?): Position {
        val pos = Position(height)
        initialState?.let {
            it.white?.let {
                for (i in 0 until it.length step 2) {
                    val stone = Util.getCoordinatesFromSGF(it, i)
                    pos.putStone(stone.x, stone.y, StoneType.WHITE)
                }
            }
            it.black?.let {
                for (i in 0 until it.length step 2) {
                    val stone = Util.getCoordinatesFromSGF(it, i)
                    pos.putStone(stone.x, stone.y, StoneType.BLACK)
                }
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
        pos.parentPosition = oldPos
        pos.lastMove = where
        pos.lastPlayerToMove = stone
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
        val removedStones = mutableSetOf<Point>()
        val neighbours = Util.getNeighbouringSpace(where, pos.boardSize)

        for (neighbour in neighbours) {
            val neighbourType = pos.getStoneAt(neighbour)
            if (neighbourType != null && neighbourType != stone) {
                doCapture(pos, neighbour, neighbourType)?.let {
                    removedStones.addAll(it)
                }
            }
        }

        if (removedStones.isNotEmpty()) {
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
            if (suicideGroup != null && suicideGroup.isNotEmpty()) {
                pos.removeStone(where)
                return null
            }
        }

        return pos
    }

    fun isIllegalSuperKO(pos: Position): Boolean {
        var historyPos = pos.parentPosition
        while(historyPos != null) {
            if(historyPos.hasTheSameStonesAs(pos)) {
                return true
            }
            historyPos = historyPos.parentPosition
        }
        return false
    }

    fun isIllegalKO(pos: Position): Boolean {
        return pos.parentPosition?.parentPosition?.hasTheSameStonesAs(pos) == true
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

    fun toggleRemoved(pos: Position, point: Point) {
        val removing = !pos.removedSpots.contains(point)
        val isStone = pos.getStoneAt(point) != null
        val toVisit = mutableListOf(point)
        val visited = mutableSetOf<Point>()
        val group = mutableListOf<Point>()

        while(!toVisit.isEmpty()) {
            val p = toVisit.removeAt(toVisit.size - 1)
            visited.add(p)
            if(isStone) {
                if(pos.getStoneAt(point) != pos.getStoneAt(p)) {
                    continue
                }
            } else if(pos.whiteTerritory.contains(point)) {
                if(!pos.whiteTerritory.contains(p)) {
                    continue
                }
            } else if(pos.blackTerritory.contains(point)) {
                if(!pos.blackTerritory.contains(p)) {
                    continue
                }
            } else if(isMarkedDame(pos, point)) {
                if(!isMarkedDame(pos, p)) {
                    continue
                }
            }
            group.add(p)
            toVisit.addAll(
                    Util.getNeighbouringSpace(p, pos.boardSize)
                            .filter { !visited.contains(it) })
        }

        if(removing) {
            pos.removedSpots.addAll(group)
        } else {
            pos.removedSpots.removeAll(group)
        }
    }

    private val handicaps = hashMapOf(
            19 to arrayOf(
                    "", // handicap = 0, B goes first, komi
                    "", // handicap = 1, B goes first, no komi
                    "pddp", // handicap > 1, W goes first, B has extra stones, no komi
                    "pppddp",
                    "ddpppddp",
                    "jjddpppddp",
                    "djpjddpppddp",
                    "djpjjjddpppddp",
                    "jdjpdjpjddpppddp",
                    "jdjpdjpjjjddpppddp"),
            13 to arrayOf(
                    "", // handicap = 0, B goes first, komi
                    "", // handicap = 1, B goes first, no komi
                    "jddj", // handicap > 1, W goes first, B has extra stones, no komi
                    "jjjddj",
                    "ddjjjddj",
                    "ggddjjjddj",
                    "dgjgddjjjddj",
                    "dgjgggddjjjddj",
                    "gdgjdgjgddjjjddj",
                    "gdgjdgjgggddjjjddj"),
            9 to arrayOf(
                    "", // handicap = 0, B goes first, komi
                    "", // handicap = 1, B goes first, komi = 3.5
                    "gccg", // handicap > 1, W goes first, B has extra stones, komi = 3.5
                    "gggccg",
                    "ccgggccg",
                    "eeccgggccg",
                    "cegeccgggccg",
                    "cegeeeccgggccg",
                    "ecegcegeccgggccg",
                    "ecegcegeeeccgggccg"
            )
    )


    fun initializePosition(boardSize: Int, handicap: Int = 0): Position {
        return Position(boardSize).apply {
            if(handicap > 1) {
                nextToMove = StoneType.WHITE
                val handicapStones = handicaps[boardSize]?.get(handicap) ?:
                    throw Exception("Handicap on custom board size not supported")
                for (i in handicapStones.indices step 2) {
                    val coords = Util.getCoordinatesFromSGF(handicapStones, i)
                    putStone(coords.x, coords.y, StoneType.BLACK)
                }
            }
            komi = determineKomi(boardSize, handicap)
        }
    }

    fun determineKomi(boardSize: Int, handicap: Int = 0): Float {
        return if(boardSize == 9) {
            if(handicap == 0) 5.5f else 3.5f
        } else {
            if(handicap == 0) 6.5f else 0.5f
        }
    }
}