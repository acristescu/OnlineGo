package io.zenandroid.onlinego.gamelogic

import android.util.Log
import androidx.core.util.lruCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.*
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
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
        val outBoard = estimate(
                pos.boardHeight, // Note: There is a bug in the estimator somewhere, width and height should be in the different order!!!
                pos.boardWidth, // Note: There is a bug in the estimator somewhere, width and height should be in the different order!!!
                inBoard,
                if(pos.nextToMove == StoneType.BLACK) 1 else -1,
                1000,
                .3f)
        val whiteTerritory = mutableSetOf<Cell>()
        val blackTerritory = mutableSetOf<Cell>()
        val removedCells = mutableSetOf<Cell>()

        for(x in 0 until pos.boardWidth) {
            for(y in 0 until pos.boardHeight) {
                when(outBoard[x * pos.boardHeight + y]) {
                    -1 -> {
                        val cell = Cell(x, y)
                        whiteTerritory += cell
                        if(pos.getStoneAt(cell) == StoneType.BLACK) {
                            removedCells += cell
                        }
                    }
                    0 -> {
                        removedCells += Cell(x, y)
                    }
                    1 -> {
                        val cell = Cell(x, y)
                        blackTerritory += cell
                        if(pos.getStoneAt(cell) == StoneType.WHITE) {
                            removedCells += cell
                        }
                    }
                }
            }
        }

        if(!scoreStones) {
            whiteTerritory.removeAll(pos.whiteStones)
            blackTerritory.removeAll(pos.blackStones)
        }
        return pos.copy(
            whiteTerritory = whiteTerritory,
            blackTerritory = blackTerritory,
            removedSpots = removedCells,
        )
    }

    data class CacheKey(
            val width: Int,
            val height: Int,
            val initialState: InitialState?,
            val whiteGoesFirst: Boolean?,
            val moves: List<Cell>?,
            val freeHandicapPlacement: Boolean?,
            val handicap: Int?,
            val removedStones: String?,
            val white_scoring_positions: String?,
            val black_scoring_positions: String?,
            val computeTerritory: Boolean,
            )

    private fun getCacheKey(game: Game, limit: Int, computeTerritory: Boolean): CacheKey {
        return CacheKey(
                width = game.width,
                height = game.height,
                initialState = game.initialState,
                whiteGoesFirst = game.whiteGoesFirst,
                moves = game.moves?.take(limit),
                freeHandicapPlacement = game.freeHandicapPlacement,
                handicap = game.handicap,
                removedStones = game.removedStones,
                white_scoring_positions = game.whiteScore?.scoring_positions,
                black_scoring_positions = game.blackScore?.scoring_positions,
                computeTerritory = computeTerritory,
        )
    }

    fun replay(game: Game, limit: Int = Int.MAX_VALUE, computeTerritory : Boolean = false): Position {
        val cacheKey = getCacheKey(game, limit, computeTerritory)
        positionsCache[cacheKey]?.let {
            return it
        }

        val pos = buildPos(
            moves = game.moves?.take(limit)?.map { Cell(it.x, it.y) } ?: emptyList(),
            nextToMove = if(game.whiteGoesFirst == true) StoneType.WHITE else StoneType.BLACK,
            boardWidth = game.width,
            boardHeight = game.height,
            blackInitialState = game.initialState?.black.toCoordinateSet(),
            whiteInitialState = game.initialState?.white.toCoordinateSet(),
            handicap = if(game.freeHandicapPlacement == true) game.handicap ?: 0 else 0,
            removedCells = game.removedStones.toCoordinateSet(),
            computeTerritory = computeTerritory,
            scoreStones = game.scoreStones ?: false,
            whiteScoringPositions = game.whiteScore?.scoring_positions,
            blackScoringPositions = game.blackScore?.scoring_positions,
            komi = game.komi,
        )

        if(pos == null) {
            Log.e(this.javaClass.simpleName, "Server returned an invalid move!!! gameId=${game.id}")
            FirebaseCrashlytics.getInstance()
                .log("E/RulesManager: Server returned an invalid move!!! gameId=${game.id}")
            return Position(game.width, game.height)
        }

        positionsCache.put(cacheKey, pos)

        return pos
    }

    private fun isMarkedDame(pos: Position, p: Cell) =
            pos.getStoneAt(p) == null && pos.removedSpots.contains(p)

    private fun isLivingStone(pos: Position, p: Cell) =
            pos.getStoneAt(p) != null && !pos.removedSpots.contains(p)

    fun Cell?.isPass() =
        this?.x == -1

    fun List<Position>.isGameOver() =
        size >= 2 && last().lastMove.isPass() && this[size - 2].lastMove.isPass()

    private val coordinatesX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")

    fun coordinateToCell(coordinate: String) : Cell =
        Cell(
            coordinatesX.indexOf(coordinate.substring(0, 1)),
            (19 - coordinate.substring(1).toInt())
        )

    fun makeMove(pos: Position, player: StoneType, move: Cell): Position? =
        buildPos(
            moves = listOf(move),
            boardWidth = pos.boardWidth,
            boardHeight = pos.boardHeight,
            whiteInitialState = pos.whiteStones,
            blackInitialState = pos.blackStones,
            removedCells = pos.removedSpots,
            nextToMove = player,
            komi = pos.komi,
            whiteCaptureCount = pos.whiteCaptureCount,
            blackCapturesCount = pos.blackCaptureCount,
        )

    fun buildPos(
        moves: List<Cell>,
        boardWidth: Int,
        boardHeight: Int,
        handicap: Int = 0,
        marks: Set<Mark> = emptySet(),
        whiteInitialState: Set<Cell> = emptySet(),
        blackInitialState: Set<Cell> = emptySet(),
        removedCells: Set<Cell> = emptySet(),
        nextToMove: StoneType = StoneType.BLACK,
        computeTerritory: Boolean = false,
        scoreStones: Boolean = false,
        whiteScoringPositions: String? = null,
        blackScoringPositions: String? = null,
        komi: Float? = null,
        whiteCaptureCount: Int = 0,
        blackCapturesCount: Int = 0,
    ): Position? {
        var nextPlayer = nextToMove
        var lastPlayer: StoneType? = null
        val whiteStones: MutableSet<Cell> = whiteInitialState.toMutableSet()
        val blackStones: MutableSet<Cell> = blackInitialState.toMutableSet()
        var whiteCaptures = whiteCaptureCount
        var blackCaptures = blackCapturesCount
        val whiteTerritory = mutableSetOf<Cell>()
        val blackTerritory = mutableSetOf<Cell>()

        moves.forEachIndexed { i, move ->
            if(move.x != -1) {
                if (whiteStones.contains(move) || blackStones.contains(move)) {
                    return null
                }
                val currentPlayerStones =
                    if (nextPlayer == StoneType.BLACK) blackStones else whiteStones
                val opponentPlayerStones =
                    if (nextPlayer == StoneType.BLACK) whiteStones else blackStones

                currentPlayerStones.add(move)

                //
                // Determine if the newly placed stone results in a capture.
                // For this, we're calling doCapture() on all the neighbours
                // of the new stones that are of opposite color
                //
                val removedStones = mutableSetOf<Cell>()
                val neighbours = Util.getNeighbouringSpace(move, boardWidth, boardHeight)

                for (neighbour in neighbours) {
                    if (opponentPlayerStones.contains(neighbour)) {
                        doCapture(
                            currentPlayerStones,
                            opponentPlayerStones,
                            neighbour,
                            boardWidth,
                            boardHeight
                        )?.let {
                            removedStones.addAll(it)
                        }
                    }
                }

                if (removedStones.isNotEmpty()) {
                    opponentPlayerStones.removeAll(removedStones)
                    when (nextPlayer) {
                        StoneType.WHITE -> whiteCaptures += removedStones.size
                        StoneType.BLACK -> blackCaptures += removedStones.size
                    }
                } else {
                    //
                    // We need to check for suicide
                    //
                    val suicideGroup = doCapture(
                        opponentPlayerStones,
                        currentPlayerStones,
                        move,
                        boardWidth,
                        boardHeight
                    )
                    if (suicideGroup != null && suicideGroup.isNotEmpty()) {
                        return null
                    }
                }
            }
            lastPlayer = nextPlayer
            if(i >= handicap - 1) {
                nextPlayer = nextPlayer.opponent
            }
        }

        //
        // WARNING: This is time consuming AF, avoid it like the plague
        //
        if(computeTerritory) {
            val alreadyVisited = mutableSetOf<Cell>()
            for (i in 0 until boardWidth) {
                (0 until boardHeight)
                    .map { Cell(i, it) }
                    .asSequence()
                    .filter { !whiteStones.contains(it) }
                    .filter { !blackStones.contains(it) }
                    .filter { !removedCells.contains(it) }
                    .filter { !whiteTerritory.contains(it) }
                    .filter { !blackTerritory.contains(it) }
                    .filter { !alreadyVisited.contains(it) }
                    .forEach {
                        val toVisit = mutableListOf(it)
                        val visited = mutableSetOf<Cell>()
                        var foundWhite = false
                        var foundBlack = false
                        while(toVisit.isNotEmpty() && !(foundBlack && foundWhite)) {
                            val p = toVisit.removeLast()
                            visited.add(p)
                            if(whiteStones.contains(p)) {
                                foundWhite = true
                                continue
                            }
                            if(blackStones.contains(p)) {
                                foundBlack = true
                                continue
                            }
                            if(removedCells.contains(p)) {
                                continue
                            }
                            toVisit.addAll(
                                Util.getNeighbouringSpace(p, boardWidth, boardHeight)
                                    .filter { !visited.contains(it) })
                        }
                        if(foundWhite && !foundBlack) {
                            whiteTerritory.addAll(
                                visited
                                    .filter { !removedCells.contains(it) }
                                    .filter { !blackTerritory.contains(it) }
                            )
                        } else if(foundBlack && !foundWhite) {
                            blackTerritory.addAll(
                                visited
                                    .filter { !removedCells.contains(it) }
                                    .filter { !whiteTerritory.contains(it) }
                            )
                        }
                        alreadyVisited.addAll(visited)
                    }
            }
            if(scoreStones) {
                whiteTerritory.addAll(whiteStones - removedCells)
                blackTerritory.addAll(blackStones - removedCells)
            }
        }

        whiteScoringPositions?.let {
            for (i in it.indices step 2) {
                whiteTerritory += Util.getCoordinatesFromSGF(it, i)
            }
        }
        blackScoringPositions?.let {
            for (i in it.indices step 2) {
                blackTerritory += Util.getCoordinatesFromSGF(it, i)
            }
        }

        return Position(
            boardWidth = boardWidth,
            boardHeight = boardHeight,
            whiteStones = whiteStones,
            blackStones = blackStones,
            whiteCaptureCount = whiteCaptures,
            blackCaptureCount = blackCaptures,
            lastMove = moves.lastOrNull(),
            lastPlayerToMove = lastPlayer,
            nextToMove = nextPlayer,
            customMarks = marks,
            removedSpots = removedCells,
            whiteTerritory = whiteTerritory,
            blackTerritory = blackTerritory,
            komi = komi,
        )
    }

    fun isIllegalKO(history: List<Position>, pos: Position) =
        history.size > 1 && history[history.lastIndex - 1].hasTheSameStonesAs(pos)

    private fun doCapture(myStones: Set<Cell>, opponentStones: Set<Cell>, origin: Cell, boardWidth: Int, boardHeight: Int): Set<Cell>? {
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
        val toVisit = LinkedList<Cell>()
        val visited = mutableSetOf<Cell>()

        toVisit.add(origin)

        while (!toVisit.isEmpty()) {
            val current = toVisit.pop()
            visited.add(current)
            val neighbours = Util.getNeighbouringSpace(current, boardWidth, boardHeight)

            for (toCheck in neighbours) {
                if(!opponentStones.contains(toCheck) && !myStones.contains(toCheck)) {
                    return null
                }
                if (opponentStones.contains(toCheck) && !visited.contains(toCheck)) {
                    toVisit.add(toCheck)
                }
            }
        }

        return visited
    }

    fun toggleRemoved(pos: Position, point: Cell): Pair<Boolean, Set<Cell>> {
        val removing = !pos.removedSpots.contains(point)
        val isStone = pos.getStoneAt(point) != null
        val toVisit = mutableListOf(point)
        val visited = mutableSetOf<Cell>()
        val group = mutableSetOf<Cell>()

        while(toVisit.isNotEmpty()) {
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
                    Util.getNeighbouringSpace(p, pos.boardWidth, pos.boardHeight)
                            .filter { !visited.contains(it) })
        }
        return removing to group
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
        val blackStones = mutableSetOf<Cell>()
        if(handicap > 1) {
            val handicapStones = handicaps[boardSize]?.get(handicap) ?:
            throw Exception("Handicap on custom board size not supported")
            for (i in handicapStones.indices step 2) {
                val coords = Util.getCoordinatesFromSGF(handicapStones, i)
                blackStones += coords
            }
        }
        return Position(
            boardWidth = boardSize,
            boardHeight = boardSize,
            komi = determineKomi(boardSize, handicap),
            nextToMove = if(handicap > 1) StoneType.WHITE else StoneType.BLACK,
            blackStones = blackStones,

        )
    }

    fun determineKomi(boardSize: Int, handicap: Int = 0): Float {
        return if(boardSize == 9) {
            if(handicap == 0) 5.5f else 3.5f
        } else {
            if(handicap == 0) 6.5f else 0.5f
        }
    }
}