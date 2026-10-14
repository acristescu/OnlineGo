package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.data.ogs.jsonElementOf
import io.zenandroid.onlinego.data.ogs.toOGSDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.time.Instant

val PERCENTILES = arrayOf(0, 477, 550, 600, 640, 671, 701, 725, 754, 774, 794, 815, 829, 847, 866, 881, 896, 912, 924, 940, 952, 969, 982, 994, 1007, 1016, 1029, 1043, 1056, 1066, 1080, 1089, 1098, 1113, 1122, 1137, 1147, 1157, 1167, 1182, 1192, 1203, 1213, 1224, 1234, 1245, 1256, 1267, 1278, 1289, 1300, 1311, 1323, 1334, 1346, 1357, 1369, 1381, 1387, 1399, 1411, 1424, 1436, 1448, 1461, 1474, 1486, 1499, 1512, 1525, 1539, 1552, 1565, 1579, 1593, 1607, 1621, 1635, 1649, 1670, 1685, 1699, 1714, 1729, 1752, 1767, 1790, 1805, 1829, 1845, 1869, 1893, 1918, 1943, 1968, 2003, 2038, 2091, 2146, 2241)
// same value used by the web client in OGS
const val PROVISIONAL_CUT_POINT: Double = 160.0;

fun getPercentile(rating: Double): Int {
    PERCENTILES.forEachIndexed { index, i -> if(rating < i) return index-1 }
    return 99
}

/**
 * Created by alex on 14/11/2017.
 */
fun json(func: JsonObjectScope.() -> Unit): JsonObject =
    JsonObjectScope().apply(func).build()

class JsonObjectScope {
    private val entries = LinkedHashMap<String, JsonElement>()

    infix operator fun String.minus(value: Any?) {
        entries[this] = jsonElementOf(value)
    }

    internal fun build() = JsonObject(entries)
}

fun createJsonArray(func: JsonArrayScope.() -> Unit): JsonArray =
    JsonArrayScope().apply(func).build()

class JsonArrayScope {
    private val elements = mutableListOf<JsonElement>()

    fun put(value: Any?) {
        elements += jsonElementOf(value)
    }

    internal fun build() = JsonArray(elements)
}

val MIN_RATING = 100.0
val MAX_RATING = 6000.0

fun egfToRank(rating: Double?) =
        rating?.let {
            ln(it.coerceIn(MIN_RATING, MAX_RATING) / 525) * 23.15
        }

fun formatRank(rank: Double?, deviation: Double? = 0.0, longFormat: Boolean = false): String {
    deviation?.let {
        when {
            it >= PROVISIONAL_CUT_POINT -> return "?"
            else -> {}
        }
    }
    return when(rank) {
        null -> "?"
        in 30f .. 100f -> "${floor(rank - 29).toInt()}${if(longFormat) " dan" else "d"}"
        in 0f .. 30f -> "${ceil(30 - rank).toInt()}${if (longFormat) " kyu" else "k"}"
        else -> ""
    }
}

private val gravatarRegex = Regex("(.*gravatar.com/avatar/[0-9a-fA-F]*+).*")
private val cdnRegex = Regex("(.*user-uploads.online-go.com.*)-\\d*\\.png")

/**
 * The OGS CDN renders each upload at a fixed set of widths. Asking for the smallest one that still
 * covers the display size keeps the download small and collapses every screen that shows the same
 * avatar onto a single cache entry.
 */
private val CDN_SIZES = intArrayOf(64, 128, 256, 512)

private fun cdnSizeFor(width: Int) = CDN_SIZES.firstOrNull { it >= width } ?: CDN_SIZES.last()

fun processGravatarURL(url: String?, width: Int): String? {
    url?.let {
        gravatarRegex.matchEntire(url)?.let { match ->
            return "${match.groupValues[1]}?s=${width}&d=404"
        }

        cdnRegex.matchEntire(url)?.let { match ->
            return "${match.groupValues[1]}-${cdnSizeFor(width)}.png"
        }
    }
    return url
}

private val SPECIAL_FLAGS = mapOf(
    "_LGBT" to "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08",

    // These have two letter iso codes, but OGS uses a custom identifier
    "_European_Union" to "\uD83C\uDDEA\uD83C\uDDFA",
    "_Kosovo" to "\uD83C\uDDFD\uD83C\uDDF0",

    // RGI subdivisions
    "_England" to "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F",
    "_Scotland" to "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC73\uDB40\uDC63\uDB40\uDC74\uDB40\uDC7F",
    "_Wales" to "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC77\uDB40\uDC6C\uDB40\uDC73\uDB40\uDC7F",

    // Just for fun
    "_Pirate" to "\uD83C\uDFF4\u200D\u2620\uFE0F",
)

fun convertCountryCodeToEmojiFlag(country: String?): String {
    SPECIAL_FLAGS[country]?.let { return it }

    if(country == null || country.length != 2 || "un" == country) {
        return "\uD83C\uDDFA\uD83C\uDDF3"
    }
    val c1 = '\uDDE6' + country[0].minus('a')
    val c2 = '\uDDE6' + country[1].minus('a')
    return "\uD83C$c1\uD83C$c2"
}

// Note: don't use the actual server time since this needs to be a pure function!
fun timeLeftForCurrentPlayer(game: Game): Long {
    game.clock?.let { clock ->
        var playerTime: Time? = null
        var playerTimeSimple: Long? = null
        when (game.playerToMoveId) {
            game.blackPlayer.id -> {
                playerTime = clock.blackTime
                playerTimeSimple = clock.blackTimeSimple
            }
            game.whitePlayer.id -> {
                playerTime = clock.whiteTime
                playerTimeSimple = clock.whiteTimeSimple
            }
        }

        val serverTimeFixed = System.currentTimeMillis()
        return computeTimeLeft(serverTimeFixed, clock, playerTimeSimple, playerTime, true, game.pausedSince).timeLeft
    }
    return 0
}


fun currentClockMillis(game: Game): Long? {
    val currentPlayer = when (game.playerToMoveId) {
        game.blackPlayer.id -> game.blackPlayer
        game.whitePlayer.id -> game.whitePlayer
        else -> null
    }
    return game.clock?.let {
        if (currentPlayer?.id == game.blackPlayer.id)
            computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true, game.pausedSince)
        else
            computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true, game.pausedSince)
    }?.timeLeft
}

sealed interface ClockFace {
    data object Unlimited : ClockFace
    data class Days(val days: Long) : ClockFace
    data class DaysHours(val days: Long, val hours: Long) : ClockFace
    data class Hours(val hours: Long) : ClockFace
    data class HoursMinutes(val hours: Long, val minutes: Long) : ClockFace
    data class MinutesSeconds(val minutes: Long, val seconds: Long) : ClockFace
    data class Seconds(val seconds: Long) : ClockFace
    data class Tenths(val tenths: Long) : ClockFace
}

fun clockFace(millis: Long): ClockFace {
    if (millis == Long.MAX_VALUE) return ClockFace.Unlimited
    var seconds = ceil((millis - 1) / 1000.0).toLong()
    val days = seconds / 86_400
    seconds -= days * 86_400
    val hours = seconds / 3_600
    seconds -= hours * 3_600
    val minutes = seconds / 60
    seconds -= minutes * 60

    return when {
        days >= 7 -> ClockFace.Days(days)
        days >= 2 && hours > 0 -> ClockFace.DaysHours(days, hours)
        days > 2 -> ClockFace.Days(days)
        days > 0 -> ClockFace.Hours(days * 24 + hours)
        hours > 0 -> ClockFace.HoursMinutes(hours, minutes)
        minutes > 0 -> ClockFace.MinutesSeconds(minutes, seconds)
        seconds > 10 -> ClockFace.Seconds(seconds)
        else -> ClockFace.Tenths((millis.coerceAtLeast(0) + 50) / 100)
    }
}

fun Long.microsToISODateTime(): String = Instant.fromEpochSeconds(
    floorDiv(MICROS_PER_SECOND),
    mod(MICROS_PER_SECOND) * NANOS_PER_MICRO,
).toOGSDateTime()

fun Instant.toEpochMicros(): Long =
    epochSeconds * MICROS_PER_SECOND + nanosecondsOfSecond / NANOS_PER_MICRO

private const val MICROS_PER_SECOND = 1_000_000L
private const val NANOS_PER_MICRO = 1_000

fun computeTimeLeft(
    serverTime: Long,
    clock: Clock,
    playerTimeSimple: Long?,
    playerTime: Time?,
    currentPlayer: Boolean,
    pausedSince: Long?,
    timeControl: TimeControl? = null,
): PlayerClock {
    val now = serverTime.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    val baseTime = clock.lastMove.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    var timeLeft = 0L
    var period: ClockPeriod? = null

    if(playerTimeSimple != null) {
        // Simple timer
        timeLeft = if(playerTimeSimple == 0L) 0 else {
            playerTimeSimple - if (currentPlayer) now else baseTime
        }
    } else if (playerTime != null) {
        timeLeft = if(currentPlayer) {
            baseTime + (playerTime.thinking_time * 1000).toLong() - now
        } else {
            (playerTime.thinking_time * 1000).toLong()
        }

        if(playerTime.moves_left != null) {

            // Canadian timer
            if(timeLeft < 0 || playerTime.thinking_time == 0.0) {
                timeLeft = baseTime + ((playerTime.thinking_time + playerTime.block_time!!) * 1000).toLong() - if(currentPlayer) now else baseTime
            }
            period = ClockPeriod.Canadian(
                (playerTime.block_time!! * 1000).toLong(),
                playerTime.moves_left
            )
        } else if(playerTime.periods != null) {

            // Byo Yomi timer
            var periodsLeft = playerTime.periods
            if(timeLeft < 0 || playerTime.thinking_time == 0.0) {
                val periodOffset =
                    ceil((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

                while(timeLeft < 0) {
                    timeLeft += (playerTime.period_time * 1000).toLong()
                }

                periodsLeft = playerTime.periods - periodOffset.toLong()
                if(periodsLeft < 0) {
                    timeLeft = 0
                }
            }
            if(!currentPlayer && timeLeft == 0L) {
                timeLeft = (playerTime.period_time!! * 1000).toLong()
            }
            period = ClockPeriod.ByoYomi(periodsLeft, (playerTime.period_time!! * 1000).toLong())
        } else if(timeControl?.time_control == "fischer"){
            period = ClockPeriod.Fischer(timeControl.time_increment!! * 1000L)
        } else {
            //absolute timer
        }
    } else {
        return PlayerClock(timeLeft = Long.MAX_VALUE)
    }

    return PlayerClock(timeLeft = timeLeft, period = period)
}

data class PlayerClock(
    val timeLeft: Long,
    val period: ClockPeriod? = null,
)

sealed interface ClockPeriod {
    data class Canadian(val blockMillis: Long, val movesLeft: Long) : ClockPeriod
    data class ByoYomi(val periodsLeft: Long, val periodMillis: Long) : ClockPeriod
    data class Fischer(val incrementMillis: Long) : ClockPeriod
}
