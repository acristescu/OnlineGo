package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util

@Immutable
data class Position(
    val boardWidth: Int,
    val boardHeight: Int,
    val whiteStones: Set<Cell> = emptySet(),
    val blackStones: Set<Cell> = emptySet(),
    val whiteCaptureCount: Int = 0,
    val blackCaptureCount: Int = 0,
    val komi: Float? = null,
    val handicap: Int = 0,
    val freeHandicapPlacement: Boolean = false,
    val whiteTerritory: Set<Cell> = emptySet(),
    val blackTerritory: Set<Cell> = emptySet(),
    val lastMove: Cell? = null,
    val lastPlayerToMove: StoneType? = null,
    val removedSpots: Set<Cell> = emptySet(),
    val nextToMove: StoneType = StoneType.BLACK,
    val customMarks: Set<Mark> = emptySet(),
    val variation: List<Cell> = emptyList(),
    val currentMoveIndex: Int = 0
) {
    fun getStoneAt(where: Cell): StoneType? =
        when {
            whiteStones.contains(where) -> StoneType.WHITE
            blackStones.contains(where) -> StoneType.BLACK
            else -> null
        }

    val whiteDeadStones: Set<Cell>
        get() = removedSpots.intersect(whiteStones)

    val blackDeadStones: Set<Cell>
        get() = removedSpots.intersect(blackStones)

    val dame: Set<Cell>
        get() = removedSpots.subtract(whiteTerritory).subtract(blackTerritory)

    companion object {
        fun fromJosekiPosition(josekiPosition: JosekiPosition): Position {
            val customMarks = josekiPosition.next_moves
                ?.filter { it.placement != null && it.placement != "pass" && it.placement != "root" }
                ?.map {
                    val childCoordinate = RulesManager.coordinateToCell(it.placement!!)
                    val overlayLabel =
                        josekiPosition.labels?.find { childCoordinate == it.placement }
                    if (overlayLabel == null) {
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
                }?.toSet()
                ?: emptySet()
            val labels = josekiPosition.labels
                ?.filter { candidate ->
                    josekiPosition.next_moves
                        ?.find {
                            it.placement != null && it.placement != "pass" && candidate.placement == RulesManager.coordinateToCell(
                                it.placement!!
                            )
                        } == null
                }
                ?.map { Mark(it.placement, it.text, it.category) }
                ?.toSet()
                ?: emptySet()
            josekiPosition.play?.let {
                val moves = when {
                    it.startsWith(".root") -> it.substring(5)
                    it.startsWith(".root.") -> it.substring(6)
                    else -> it
                }.split('.')
                    .filter { it != "" }
                    .map {
                        if (it == "pass") {
                            Cell(-1, -1)
                        } else {
                            RulesManager.coordinateToCell(it)
                        }
                    }

                return RulesManager.buildPos(
                    moves,
                    19,
                    19,
                    marks = customMarks + labels,
                ) ?: Position(19, 19)
            }
            return Position(
                boardHeight = 19,
                boardWidth = 19,
            )
        }

    }

    fun hasTheSameStonesAs(other: Position) =
        whiteStones == other.whiteStones && blackStones == other.blackStones
}

@Immutable
data class Mark(
    val placement: Cell,
    val text: String?,
    val category: PlayCategory?
)

@Immutable
data class Cell(
    val x: Int,
    val y: Int
) {
    val leftNeighbour: Cell
        get() = Cell(x - 1, y)
    val rightNeighbour: Cell
        get() = Cell(x + 1, y)
    val topNeighbour: Cell
        get() = Cell(x, y - 1)
    val bottomNeighbour: Cell
        get() = Cell(x, y + 1)
    val isPass: Boolean
        get() = this == PASS

    companion object {
        fun fromSGF(s: String) =
            Util.getCoordinatesFromSGF(s)

        fun fromGTP(s: String, boardSize: Int) =
            Util.getCoordinatesFromGTP(s, boardSize)

        val PASS = Cell(-1, -1)
    }
}
