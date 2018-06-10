package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import io.zenandroid.onlinego.model.ogs.Game
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
        var clock: Clock?,

        var phase: Game.Phase?,

        var komi: Float?
        ) {
    @Ignore
    var json: GameData? = null

    companion object {
        fun fromOGSGame(game: Game): DbGame {
            val whiteRating = ((((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double)
                    ?: game.gamedata?.players?.white?.ratings?.overall?.rating
                    ?: game.players?.white?.ratings?.overall?.rating
            val blackRating = (((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                    ?: game.gamedata?.players?.black?.ratings?.overall?.rating
                    ?: game.players?.black?.ratings?.overall?.rating
            val players = game.json?.players ?: game.gamedata?.players ?: game.players
            val gamedata = game.json ?: game.gamedata

            val whitePlayer = DbPlayer(
                    id = (players?.white?.id ?: game.whiteId)!!,
                    username = players?.white?.username ?: (game.white as? Map<*,*>)?.get("username").toString(),
                    country = players?.white?.country ?: game.whitePlayer?.country ?: game.players?.white?.country,
                    icon = players?.white?.icon ?: game.whitePlayer?.icon ?: game.players?.white?.icon,
                    rating = whiteRating
            )
            val blackPlayer = DbPlayer(
                    id = (players?.black?.id ?: game.blackId)!!,
                    username = players?.black?.username ?: (game.black as? Map<*,*>)?.get("username").toString(),
                    country = players?.black?.country ?: game.blackPlayer?.country ?: game.players?.black?.country,
                    icon = players?.black?.icon ?: game.blackPlayer?.icon ?: game.players?.black?.icon,
                    rating = blackRating
            )
            return DbGame(
                    id = game.id,
                    width = game.width,
                    height = game.height,
                    outcome = game.outcome,
                    playerToMoveId = gamedata?.clock?.current_player,
                    whiteLost = game.white_lost,
                    blackLost = game.black_lost,
                    initialState = gamedata?.initial_state,
                    whiteGoesFirst = gamedata?.initial_player == "white",
                    moves = gamedata?.moves?.apply { forEach { if(it.size == 3) it.removeAt(it.lastIndex) } },
                    removedStones = gamedata?.removed,
                    whiteScore = gamedata?.score?.white,
                    blackScore = gamedata?.score?.black,
                    whitePlayer = whitePlayer,
                    blackPlayer = blackPlayer,
                    clock = Clock.fromOGSClock(gamedata?.clock),
                    phase = gamedata?.phase,
                    komi = game.komi
            )
        }
    }
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
        var blackTime: Time?,

        var startMode: Boolean?
) {
    companion object {
        fun fromOGSClock(clock: io.zenandroid.onlinego.ogs.Clock?): Clock? =
                clock?.let {
                    Clock(
                            lastMove = clock.last_move,
                            expiration = clock.expiration,
                            now = clock.now,
                            receivedAt = if (clock.receivedAt != 0L) clock.receivedAt else System.currentTimeMillis(),
                            whiteTimeSimple = clock.whiteTimeSimple,
                            whiteTime = clock.whiteTime,
                            blackTimeSimple = clock.blackTimeSimple,
                            blackTime = clock.blackTime,
                            startMode = clock.start_mode
                    )
                }
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

