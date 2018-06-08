package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
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
        var blackScore: Score?,

        @Embedded(prefix = "white_")
        var whitePlayer: DbPlayer,

        @Embedded(prefix = "black_")
        var blackPlayer: DbPlayer,

        @Embedded
        var clock: Clock
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

data class Clock(
        var lastMove: Long,
        var expiration: Long? = null,
        var now: Long,
        var receivedAt: Long,

        var whiteTimeSimple: Long?,

        @Embedded(prefix = "white_")
        var whiteTime: Time?,

        var blackTimeSimple: Long?,

        @Embedded(prefix = "black_")
        var blackTime: Time?
) {
    companion object {
        fun fromOGSClock(clock: io.zenandroid.onlinego.ogs.Clock): Clock =
                Clock(
                        lastMove = clock.last_move,
                        expiration = clock.expiration,
                        now = clock.now,
                        receivedAt = if(clock.receivedAt != 0L) clock.receivedAt else System.currentTimeMillis(),
                        whiteTimeSimple = clock.whiteTimeSimple,
                        whiteTime = clock.whiteTime,
                        blackTimeSimple = clock.blackTimeSimple,
                        blackTime = clock.blackTime
                )
    }
}

data class Time(
        val thinking_time: Long,
        val skip_bonus: Boolean? = null,
        val block_time: Long? = null,
        val periods: Long? = null,
        val period_time: Long? = null,
        val moves_left: Long? = null
) {
    companion object {
        fun fromMap(map:Map<*, *>): Time {
            return Time(
                    (map["thinking_time"] as Double).toLong(),
                    map["skip_bonus"] as? Boolean,
                    (map["block_time"] as Double?)?.toLong(),
                    (map["periods"] as Double?)?.toLong(),
                    (map["period_time"] as Double?)?.toLong(),
                    (map["moves_left"] as Double?)?.toLong()
            )
        }
    }
}

