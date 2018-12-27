package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.crashlytics.android.Crashlytics
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.TimeControl

/**
 * Created by 44108952 on 05/06/2018.
 */
@Entity
data class Game(
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
        var whitePlayer: Player,

        @Embedded(prefix = "black_")
        var blackPlayer: Player,

        @Embedded
        var clock: Clock?,

        var phase: Phase?,

        var komi: Float?,

        var ended: Long?,

        var freeHandicapPlacement: Boolean?,
        var handicap: Int?,
        var undoRequested: Int?,
        var scoreStones: Boolean?,
        val name: String?,
        val rules: String?,
        val ranked: Boolean?,

        @Embedded(prefix = "initial_state_")
        val timeControl: TimeControl?
        ) {
    @Ignore
    var json: GameData? = null

    companion object {
        fun fromOGSGame(game: OGSGame): Game {
            val whiteRating = ((((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double)
                    ?: game.gamedata?.players?.white?.ratings?.overall?.rating
                    ?: game.players?.white?.ratings?.overall?.rating
            val blackRating = (((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                    ?: game.gamedata?.players?.black?.ratings?.overall?.rating
                    ?: game.players?.black?.ratings?.overall?.rating
            val players = game.json?.players ?: game.gamedata?.players ?: game.players
            val gamedata = game.json ?: game.gamedata

            val whitePlayer = Player(
                    id = (players?.white?.id ?: game.whiteId)!!,
                    username = players?.white?.username ?: (game.white as? Map<*,*>)?.get("username").toString(),
                    country = players?.white?.country ?: game.whitePlayer?.country ?: game.players?.white?.country,
                    icon = players?.white?.icon ?: game.whitePlayer?.icon ?: game.players?.white?.icon,
                    rating = whiteRating,
                    acceptedStones = players?.white?.accepted_stones
            )
            val blackPlayer = Player(
                    id = (players?.black?.id ?: game.blackId)!!,
                    username = players?.black?.username ?: (game.black as? Map<*,*>)?.get("username").toString(),
                    country = players?.black?.country ?: game.blackPlayer?.country ?: game.players?.black?.country,
                    icon = players?.black?.icon ?: game.blackPlayer?.icon ?: game.players?.black?.icon,
                    rating = blackRating,
                    acceptedStones = players?.black?.accepted_stones
            )

            val isRanked : Boolean? = when(gamedata?.ranked) {
                null -> null
                is Double -> gamedata.ranked != 0.0
                is Boolean -> gamedata.ranked as Boolean
                else -> {
                    Crashlytics.log("gamedata.ranked has unexpected value: ${gamedata.ranked}")
                    null
                }
            }
            return Game(
                    id = game.id,
                    width = game.width,
                    height = game.height,
                    outcome = game.outcome,
                    playerToMoveId = gamedata?.clock?.current_player,
                    whiteLost = game.white_lost,
                    blackLost = game.black_lost,
                    initialState = gamedata?.initial_state,
                    whiteGoesFirst = gamedata?.initial_player == "white",
                    moves = gamedata?.moves?.map { mutableListOf(it[0].toInt(), it[1].toInt()) }?.toMutableList() ?: mutableListOf(),
                    removedStones = gamedata?.removed,
                    whiteScore = gamedata?.score?.white,
                    blackScore = gamedata?.score?.black,
                    whitePlayer = whitePlayer,
                    blackPlayer = blackPlayer,
                    clock = Clock.fromOGSClock(gamedata?.clock),
                    phase = gamedata?.phase,
                    komi = game.komi,
                    ended = game.ended?.time,
                    freeHandicapPlacement = gamedata?.free_handicap_placement,
                    handicap = game.handicap,
                    undoRequested = gamedata?.undo_requested,
                    scoreStones = gamedata?.score_stones,
                    name = gamedata?.game_name,
                    rules = gamedata?.rules,
                    ranked = isRanked,
                    timeControl = gamedata?.time_control
            )
        }
    }
}

