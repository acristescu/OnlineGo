package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.utils.toEpochMicros

/**
 * Created by alex on 05/06/2018.
 */
@Entity
@Immutable
data class Game(
    @PrimaryKey val id: Long,
    val width: Int,
    val height: Int,
    val playerToMoveId: Long?,
    val outcome: String?,
    val whiteLost: Boolean?,
    val blackLost: Boolean?,

    @Embedded(prefix = "initial_state_")
        val initialState: InitialState?,

    val whiteGoesFirst: Boolean?,
    val moves: List<Cell>?,
    val removedStones: String?,

    @Embedded(prefix = "white_")
        val whiteScore: Score?,

    @Embedded(prefix = "black_")
        val blackScore: Score?,

    @Embedded(prefix = "white_")
        val whitePlayer: Player,

    @Embedded(prefix = "black_")
        val blackPlayer: Player,

    @Embedded
        val clock: Clock?,

    val phase: Phase?,

    val komi: Float?,

    // Note: This is microseconds (not milliseconds!!!) since the epoch
    val ended: Long?,

    val freeHandicapPlacement: Boolean?,
    val handicap: Int?,
    val undoRequested: Int?,
    val scoreAGAHandicap: Boolean?,
    val scoreHandicap: Boolean?,
    val scorePasses: Boolean?,
    val scorePrisoners: Boolean?,
    val scoreStones: Boolean?,
    val scoreTerritory: Boolean?,
    val scoreTerritoryInSeki: Boolean?,
    val name: String?,
    val rules: String?,
    val ranked: Boolean?,
    val disableAnalysis: Boolean?,

    val pausedSince: Long? = null,

    @Embedded(prefix = "initial_state_")
    val timeControl: TimeControl?,
    val messagesCount: Int? = null,

    @Embedded(prefix = "pause_")
    val pauseControl: PauseControl? = null,

    var annulled: Boolean? = null,
) {
    @Ignore
    @Transient
    lateinit var position: Position

    @Ignore
    val json: GameData? = null

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

            val whiteDeviation = ((((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("deviation") as? Double)
                ?: game.gamedata?.players?.white?.ratings?.overall?.deviation
                ?: game.players?.white?.ratings?.overall?.deviation

            val blackDeviation = ((((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("deviation") as? Double)
                ?: game.gamedata?.players?.black?.ratings?.overall?.deviation
                ?: game.players?.black?.ratings?.overall?.deviation

            val whitePlayer = Player(
                id = (players?.white?.id ?: game.whiteId)!!,
                username = players?.white?.username ?: (game.white as? Map<*,*>)?.get("username").toString(),
                country = players?.white?.country ?: game.whitePlayer?.country ?: game.players?.white?.country,
                icon = players?.white?.icon ?: game.whitePlayer?.icon ?: game.players?.white?.icon,
                rating = whiteRating,
                acceptedStones = players?.white?.accepted_stones,
                ui_class = players?.white?.ui_class,
                historicRating = game.historical_ratings?.white?.ratings?.overall?.rating,
                deviation = whiteDeviation
            )
            val blackPlayer = Player(
                id = (players?.black?.id ?: game.blackId)!!,
                username = players?.black?.username ?: (game.black as? Map<*,*>)?.get("username").toString(),
                country = players?.black?.country ?: game.blackPlayer?.country ?: game.players?.black?.country,
                icon = players?.black?.icon ?: game.blackPlayer?.icon ?: game.players?.black?.icon,
                rating = blackRating,
                acceptedStones = players?.black?.accepted_stones,
                ui_class = players?.black?.ui_class,
                historicRating = game.historical_ratings?.black?.ratings?.overall?.rating,
                deviation = blackDeviation
            )

            val isRanked : Boolean? = when(gamedata?.ranked) {
                null -> null
                is Double -> gamedata.ranked != 0.0
                is Boolean -> gamedata.ranked as Boolean
                else -> {
                    FirebaseCrashlytics.getInstance().log("gamedata.ranked has unexpected value: ${gamedata.ranked}")
                    null
                }
            }

            val hasOutcome = !game.outcome.isNullOrEmpty()
            val whiteLost = when {
                hasOutcome && game.white_lost != null -> {
                    game.white_lost
                }
                hasOutcome && gamedata?.winner == game.blackId -> {
                    true
                }
                hasOutcome && gamedata?.winner == game.whiteId -> {
                    false
                }
                else -> {
                    null
                }
            }
            val blackLost = when {
                hasOutcome && game.black_lost != null -> {
                    game.black_lost
                }
                hasOutcome && gamedata?.winner == game.whiteId -> {
                    true
                }
                hasOutcome && gamedata?.winner == game.blackId -> {
                    false
                }
                else -> {
                    null
                }
            }

            val pauseControl = gamedata?.pause_control?.let {
                //pause_control: {weekend: true, vacation-43936: true}
                PauseControl(
                    weekend = it["weekend"] != null,
                    moderator = it["moderator_paused"] != null,
                    server = it["server"] != null || it["system"] != null,
                    stoneRemoval = it["stone-removal"] != null,
                    pausedByThirdParty = it["paused"] != null,
                    vacationWhite = it["vacation-${whitePlayer.id}"] != null,
                    vacationBlack = it["vacation-${blackPlayer.id}"] != null,
                )
            }
            return Game(
                id = game.id,
                width = game.width,
                height = game.height,
                outcome = if(game.outcome.isNullOrEmpty()) null else game.outcome,
                playerToMoveId = gamedata?.clock?.current_player,
                whiteLost = whiteLost,
                blackLost = blackLost,
                initialState = gamedata?.initial_state,
                whiteGoesFirst = gamedata?.initial_player == "white",
                moves = gamedata?.moves?.map { Cell((it[0] as Double).toInt(), (it[1] as Double).toInt()) } ?: emptyList(),
                removedStones = gamedata?.removed,
                whiteScore = gamedata?.score?.white,
                blackScore = gamedata?.score?.black,
                whitePlayer = whitePlayer,
                blackPlayer = blackPlayer,
                clock = Clock.fromOGSClock(gamedata?.clock),
                phase = gamedata?.phase ?: if(hasOutcome) Phase.FINISHED else null,
                komi = if(game.komi == null) gamedata?.komi else game.komi,
                ended = game.ended?.toEpochMicros(),
                freeHandicapPlacement = gamedata?.free_handicap_placement,
                handicap = game.handicap,
                undoRequested = gamedata?.undo_requested,
                scoreAGAHandicap = gamedata?.aga_handicap_scoring,
                scoreHandicap = gamedata?.score_handicap,
                scorePasses = gamedata?.score_passes,
                scorePrisoners = gamedata?.score_prisoners,
                scoreStones = gamedata?.score_stones,
                scoreTerritory = gamedata?.score_territory,
                scoreTerritoryInSeki = gamedata?.score_territory_in_seki,
                name = gamedata?.game_name ?: game.name,
                rules = gamedata?.rules ?: game.rules,
                ranked = isRanked,
                timeControl = gamedata?.time_control,
                disableAnalysis = gamedata?.disable_analysis,
                pausedSince = gamedata?.paused_since,
                pauseControl = pauseControl,
                annulled = game.annulled,
            )
        }

        fun sampleData() =
            Game(
                id = 1968L,
                width = 19,
                height = 19,
                outcome = null,
                playerToMoveId = 1L,
                whiteLost = false,
                blackLost = false,
                initialState = null,
                whiteGoesFirst = true,
                moves = listOf(Cell(3, 3), Cell(13, 13)),
                removedStones = "",
                whiteScore = Score(komi = 5.5f, prisoners = 3),
                blackScore = Score(prisoners = 1),
                whitePlayer = Player(id = 1L, username = "Bula", rating = 1500.5, country = "UK", icon = null, acceptedStones = null, ui_class = null, historicRating = 1550.0, deviation = 50.0),
                blackPlayer = Player(id = 0L, username = "Playa", rating = 1600.5, country = "UK", icon = null, acceptedStones = null, ui_class = null, historicRating = 1550.0, deviation = 50.0),
                clock = Clock(lastMove = 120L, receivedAt = 120L, whiteTime = null, whiteTimeSimple = null, startMode = false, blackTime = null, blackTimeSimple = null),
                phase = null,
                komi = 5.5f,
                ended = null,
                freeHandicapPlacement = false,
                handicap = null,
                undoRequested = null,
                scoreAGAHandicap = false,
                scoreHandicap = false,
                scorePasses = false,
                scorePrisoners = true,
                scoreStones = false,
                scoreTerritory = true,
                scoreTerritoryInSeki = false,
                name = "Game name",
                rules = "Japanese",
                ranked = false,
                timeControl = null,
                disableAnalysis = false,
                pausedSince = null,
                pauseControl = null,
                annulled = null
            )
    }
}
