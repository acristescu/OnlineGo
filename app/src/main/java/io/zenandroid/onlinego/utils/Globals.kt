package io.zenandroid.onlinego.utils

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.PluralsRes
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.ogs.TimeControl
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.context.GlobalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

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
fun json(func: JsonObjectScope.() -> Unit): JSONObject {
    val obj = JSONObject()
    object: JsonObjectScope{
        override val json = obj
    }.func()
    return obj
}

interface JsonObjectScope {
    val json: JSONObject

    infix operator fun String.minus(value: Any?) {
        json.put(this, value)
    }
}

fun createJsonArray(func: JSONArray.() -> Unit): JSONArray {
    val obj = JSONArray()
    func(obj)
    return obj
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

private val gravatarRegex = Pattern.compile("(.*gravatar.com/avatar/[0-9a-fA-F]*+).*")
private val cdnRegex = Pattern.compile("(.*user-uploads.online-go.com.*)-\\d*\\.png")

fun processGravatarURL(url: String?, width: Int): String? {
    url?.let {
        var matcher = gravatarRegex.matcher(url)
        if(matcher.matches()) {
            return "${matcher.group(1)}?s=${width}&d=404"
        }

      matcher = cdnRegex.matcher(url)
        if(matcher.matches()) {
            val desired = max(512.0, 2.0.pow(ln(width.toDouble()) / ln(2.0))).toInt()
            return "${matcher.group(1)}-${desired}.png"
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


fun calculateTimer(game: Game): String {
    val currentPlayer = when (game.playerToMoveId) {
        game.blackPlayer.id -> game.blackPlayer
        game.whitePlayer.id -> game.whitePlayer
        else -> null
    }
    val timerDetails = game.clock?.let {
        if (currentPlayer?.id == game.blackPlayer.id)
            computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true, game.pausedSince)
        else
            computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true, game.pausedSince)
    }
    return timerDetails?.firstLine ?: ""
}

fun formatMillis(millis: Long): String {
    var seconds = ceil((millis - 1) / 1000.0).toLong()
    val days = seconds / 86_400
    seconds -= days * 86_400
    val hours = seconds / 3_600
    seconds -= hours * 3_600
    val minutes = seconds / 60
    seconds -= minutes * 60

    return when {
        days >= 7 -> "%d days".format(days)
        days >= 2 && hours > 0 -> "%dd %dh".format(days, hours)
        days > 2 -> "%d day%s".format(days, plural(days))
        days > 0 -> "%dh".format(days * 24 + hours)
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%d : %02d".format(minutes, seconds)
        seconds > 10 -> "%02ds".format(seconds)
        millis > 0 -> "%.1fs".format(millis / 1000f)
        else -> "0.0"
    }
}

fun plural(number: Long) = if(number != 1L) "s" else ""

fun timeControlDescription(resources: Resources, timeControl: TimeControl): String {

    val system = timeControl.system ?: timeControl.time_control
    var desc = when(system) {
        "simple" -> resources.getString(
            R.string.time_control_simple,
            formatSeconds(resources, timeControl.per_move)
        )

        "fischer" -> resources.getString(
            R.string.time_control_fischer,
            formatSeconds(resources, timeControl.initial_time),
            formatSeconds(resources, timeControl.time_increment),
            formatSeconds(resources, timeControl.max_time)
        )

        "byoyomi" -> {
            val periods = timeControl.periods ?: 0
            resources.getQuantityString(
                R.plurals.time_control_byoyomi,
                periods,
                formatSeconds(resources, timeControl.main_time),
                periods,
                formatSeconds(resources, timeControl.period_time)
            )
        }

        "canadian" -> {
            val stones = timeControl.stones_per_period ?: 0
            resources.getQuantityString(
                R.plurals.time_control_canadian,
                stones,
                formatSeconds(resources, timeControl.main_time),
                formatSeconds(resources, timeControl.period_time),
                stones
            )
        }

        "absolute" -> resources.getString(
            R.string.time_control_absolute,
            formatSeconds(resources, timeControl.total_time)
        )

        "none" -> resources.getString(R.string.time_control_none)
        else -> resources.getString(R.string.time_control_unknown)
    }

    if(timeControl.pause_on_weekends == true) {
        desc += resources.getString(R.string.time_control_pauses_on_weekends)
    }

    return desc
}

fun formatSeconds(resources: Resources, seconds: Int?): String {
    seconds?.let {
        var s = it.toDouble()
        val weeks = (s / (86400 * 7)).toLong()
        s -= weeks . toInt () * 86400 * 7
        val days = (s / 86400).toLong()
        s -= days * 86400
        val hours = (s / 3600).toLong()
        s -= hours * 3600
        val minutes = (s / 60).toLong()
        s -= minutes * 60

        return when {
            weeks > 0 -> resources.duration(
                R.plurals.duration_weeks,
                weeks,
                R.plurals.duration_days,
                days
            )

            days > 0 -> resources.duration(
                R.plurals.duration_days,
                days,
                R.plurals.duration_hours,
                hours
            )

            hours > 0 -> resources.duration(
                R.plurals.duration_hours,
                hours,
                R.plurals.duration_minutes,
                minutes
            )

            minutes > 0 -> resources.duration(
                R.plurals.duration_minutes,
                minutes,
                R.plurals.duration_seconds,
                s.toLong()
            )

            else -> resources.durationUnit(R.plurals.duration_seconds, s.toLong())
        }
    }
    return resources.getString(R.string.duration_unknown)
}

private fun Resources.durationUnit(@PluralsRes unit: Int, value: Long): String =
    getQuantityString(unit, value.toInt(), value)

/** Formats [value] of [unit], appending [remainderValue] of [remainderUnit] when it is not zero. */
private fun Resources.duration(
    @PluralsRes unit: Int,
    value: Long,
    @PluralsRes remainderUnit: Int,
    remainderValue: Long,
): String {
    val head = durationUnit(unit, value)
    return if (remainderValue > 0) {
        getString(R.string.duration_two_units, head, durationUnit(remainderUnit, remainderValue))
    } else head
}

fun Long.microsToISODateTime(): String {
    val instant = Instant.EPOCH.plus(this, ChronoUnit.MICROS)
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withLocale( Locale.US )
            .withZone( ZoneId.of("America/New_York") )
            .format(instant)
}

fun Instant.toEpochMicros(): Long
        = ChronoUnit.MICROS.between(Instant.EPOCH, this)

fun computeTimeLeft(
    serverTime: Long,
    clock: Clock,
    playerTimeSimple: Long?,
    playerTime: Time?,
    currentPlayer: Boolean,
    pausedSince: Long?,
    timeControl: TimeControl? = null,
): TimerDetails {
    val now = serverTime.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    val baseTime = clock.lastMove.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    var timeLeft = 0L
    var secondLine: String? = null

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
            secondLine = "+${formatMillis((playerTime.block_time!! * 1000).toLong())} / ${playerTime.moves_left}"
        } else if(playerTime.periods != null) {

            // Byo Yomi timer
            var periodsLeft = playerTime.periods
            if(timeLeft < 0 || playerTime.thinking_time == 0.0) {
                val periodOffset = Math.ceil((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

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
            secondLine = "$periodsLeft x ${formatMillis((playerTime.period_time!! * 1000).toLong())}"
        } else if(timeControl?.time_control == "fischer"){
            secondLine = "+ ${formatMillis(timeControl.time_increment!! * 1000L)} / move"
        } else {
            //absolute timer
        }
    } else {
        // No timer
        return TimerDetails(
            expired = false,
            firstLine = "∞",
            secondLine = null,
            timeLeft = Long.MAX_VALUE
        )
    }

    return TimerDetails(
        expired = timeLeft <= 0,
        firstLine = formatMillis(timeLeft),
        secondLine = secondLine,
        timeLeft = timeLeft
    )
}

data class TimerDetails (
    var expired: Boolean,
    var firstLine: String? = null,
    var secondLine: String? = null,
    var timeLeft: Long
)

fun toastException(t: Throwable, long: Boolean = false) {
    if (!BuildConfig.DEBUG) return

    val context: Context = GlobalContext.get().get()
    val length = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(context, t.toString(), length).show()
}
