package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.*
import io.zenandroid.onlinego.model.ogs.GameData

/**
 * Created by 44108952 on 05/06/2018.
 */
@Entity
data class DbGame(
        @PrimaryKey var id: Long,
        var width: Int,
        var height: Int,
        var playerToMoveId: Long?,
        var outcome: String?,
        var whiteLost: Boolean?,
        var blackLost: Boolean?,

        @Embedded(prefix = "initial_state_")
        var initialState: InitialState?,

        var whiteGoesFirst: Boolean?,
        var moves: MutableList<MutableList<Int>>?,
        var removedStones: String?,

        @Embedded(prefix = "white_")
        var whiteScore: Score?,

        @Embedded(prefix = "black_")
        var blackScore: Score?
) {
    @Ignore
    var json: GameData? = null
}

data class InitialState (
        var black: String? = null,
        var white: String? = null
)

data class Score (
        var handicap: Double? = null,
        var komi: Double? = null,
        var prisoners: Int? = null,
        var scoring_positions: String? = null,
        var stones: Int? = null,
        var territory: Int? = null,
        var total: Double? = null
)
