package io.zenandroid.onlinego.utils

import android.util.Log
import io.zenandroid.onlinego.game.GamePresenter
import io.zenandroid.onlinego.model.local.Clock
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.local.Time
import io.zenandroid.onlinego.ogs.TimeControl
import kotlinx.android.synthetic.main.view_player_details.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Math.ceil
import java.lang.Math.log
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Created by alex on 14/11/2017.
 */
fun createJsonObject(func: JSONObject.() -> Unit): JSONObject {
    val obj = JSONObject()
    func(obj)
    return obj
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
            log(it.coerceIn(MIN_RATING, MAX_RATING) / 850.0) / 0.032
        }

fun formatRank(rank: Double?) =
    when(rank) {
        null -> "?"
        in 0 until 30 -> "${ceil(30 - rank).toInt()}k"
        in 30 .. 100 -> "${ceil(rank - 29).toInt()}d"
        else -> ""
    }

private val gravatarRegex = Pattern.compile("(.*gravatar.com/avatar/[0-9a-fA-F]*+).*")
private val rackcdnRegex = Pattern.compile("(.*rackcdn.com.*)-\\d*\\.png")

fun processGravatarURL(url: String?, width: Int): String? {
    url?.let {
        var matcher = gravatarRegex.matcher(url)
        if(matcher.matches()) {
            return "${matcher.group(1)}?s=${width}&d=404"
        }

        matcher = rackcdnRegex.matcher(url)
        if(matcher.matches()) {
            val desired = max(512.0, 2.0.pow(ln(width.toDouble()) / ln(2.0))).toInt()
            return "${matcher.group(1)}-${desired}.png"
        }
    }
    return url
}

fun convertCountryCodeToEmojiFlag(country: String?): String {
    if(country == null || country.length != 2 || "un" == country) {
        return "\uD83C\uDDFA\uD83C\uDDF3"
    }
    val c1 = '\uDDE6' + country[0].minus('a')
    val c2 = '\uDDE6' + country[1].minus('a')
    return "\uD83C$c1\uD83C$c2"
}

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

        return computeTimeLeft(clock, playerTimeSimple, playerTime, true).timeLeft ?: 0
    }
    return 0
}

fun computeTimeLeft(clock: Clock, playerTimeSimple: Long?, playerTime: Time?, currentPlayer: Boolean): GamePresenter.TimerDetails {
    val timer = GamePresenter.TimerDetails()

    val now = System.currentTimeMillis()
    if(clock.receivedAt == 0L) {
        clock.receivedAt = now
    }
    var nowDelta = clock.receivedAt - (clock.now ?: 0)
    if(nowDelta > 100000) { // sanity check
        nowDelta = 0
    }
    val baseTime = clock.lastMove + nowDelta
    var timeLeft = 0L
    if(playerTimeSimple != null) {
        // Simple timer
        timeLeft = if(playerTimeSimple == 0L) 0 else {
            playerTimeSimple - if (currentPlayer) now else baseTime
        }
    } else if (playerTime != null) {
        timeLeft = baseTime + playerTime.thinking_time * 1000 - if(currentPlayer) now else baseTime
        if(playerTime.moves_left != null) {

            // Canadian timer
            if(timeLeft < 0 || playerTime.thinking_time == 0L) {
                timeLeft = baseTime + (playerTime.thinking_time + playerTime.block_time!!) * 1000 - if(currentPlayer) now else baseTime
            }
            timer.secondLine = "+${formatMillis(playerTime.block_time!! * 1000)} / ${playerTime.moves_left}"
        } else if(playerTime.periods != null) {

            // Byo Yomi timer
            var periodsLeft = playerTime.periods
            if(timeLeft < 0 || playerTime.thinking_time == 0L) {
                val periodOffset = Math.ceil((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

                while(timeLeft < 0) {
                    timeLeft += playerTime.period_time * 1000
                }

                periodsLeft = playerTime.periods - periodOffset.toLong()
                if(periodsLeft < 0) {
                    timeLeft = 0
                }
            }
            if(!currentPlayer && timeLeft == 0L) {
                timeLeft = playerTime.period_time!! * 1000
            }
            timer.secondLine = "$periodsLeft x ${formatMillis(playerTime.period_time!! * 1000)}"
        }
    } else {
        Log.e("GamePresenter", "Clock object has neither simple time or complex time")
    }

    timer.expired = timeLeft <= 0
    timer.firstLine = formatMillis(timeLeft)
    timer.timeLeft = timeLeft
    return timer
}

fun formatMillis(millis: Long): String = when {
    millis < 10_000 -> "%.1fs".format(millis / 1000f)
    millis < 60_000 -> "%.0fs".format(millis / 1000f)
    millis < 3_600_000 -> "%d : %02d".format(millis / 60_000, (millis % 60_000) / 1000)
    millis < 24 * 3_600_000 -> "%dh %02dm".format(millis / 3_600_000, (millis % 3_600_000) / 60_000)
    millis < 7 * 24 * 3_600_000 -> "%d day%s".format(millis / 86_400_000, plural(millis / 86_400_000))
    else -> "%d week%s".format(millis/(7 * 24 * 3_600_000), plural(millis/(7 * 24 * 3_600_000)))
}

fun plural(number: Long) = if(number != 1L) "s" else ""

fun timeControlDescription(timeControl: TimeControl): String {

    val system = timeControl.system ?: timeControl.time_control
    var desc = when(system) {
        "simple" -> "Simple: ${formatSeconds(timeControl.per_move)} per move."
        "fischer" -> "Fischer: Clock starts with ${formatSeconds(timeControl.initial_time)} and increments by ${formatSeconds(timeControl.time_increment)} per move up to a maximum of ${formatSeconds(timeControl.max_time)}."
        "byoyomi" -> "Japanese Byo-Yomi: Clock starts with ${formatSeconds(timeControl.main_time)} main time, followed by ${timeControl.periods} periods of ${formatSeconds(timeControl.period_time)} each."
        "canadian" -> "Canadian Byo-Yomi: Clock starts with ${formatSeconds(timeControl.main_time)} main time, followed by ${formatSeconds(timeControl.period_time)} per ${timeControl.stones_per_period} stones."
        "absolute" -> "Absolute: ${formatSeconds(timeControl.total_time)} total play time per player."
        "none" -> "No time limits."
        else -> "?"
    }

    if(timeControl.pause_on_weekends == true) {
        desc += " Pauses on weekends."
    }

    return desc
}

fun pluralButNotZero(number: Long, suffix: String) =
    if(number > 0) {
        " $number $suffix${plural(number)}"
    } else {
        ""
    }

fun formatSeconds(seconds: Int?): String {
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
            weeks > 0 -> "$weeks week${plural(weeks)}${pluralButNotZero(days, "day")}"
            days > 0 -> "$days day${plural(days)}${pluralButNotZero(hours, "hour")}"
            hours > 0 -> "$hours hour${plural(hours)}${pluralButNotZero(minutes, "minute")}"
            minutes > 0 -> "$minutes minute${plural(minutes)}${pluralButNotZero(s.toLong(), "seconds")}"
            else -> "${s.toLong()} second${plural(s.toLong())}"
        }
    }
    return "?"
}
