package io.zenandroid.onlinego.utils

import android.util.Log
import io.zenandroid.onlinego.ui.screens.game.GamePresenter
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.context.GlobalContext.get
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.lang.Math.log
import java.util.*
import java.util.regex.Pattern
import kotlin.math.*

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
            ln(it.coerceIn(MIN_RATING, MAX_RATING) / 525) * 23.15
        }

fun formatRank(rank: Double?) =
        when(rank) {
            null -> "?"
            in 30f .. 100f -> "${ceil(rank - 29).toInt()}d"
            in 0f .. 30f -> "${ceil(30 - rank).toInt()}k"
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
    pausedSince: Long?
): GamePresenter.TimerDetails {
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
        } else {
            // absolute timer or fischer timer, nothing to do
        }
    } else {
        // No timer
        return GamePresenter.TimerDetails(
            expired = false,
            firstLine = "∞",
            secondLine = null,
            timeLeft = Long.MAX_VALUE
        )
    }

    return GamePresenter.TimerDetails(
        expired = timeLeft <= 0,
        firstLine = formatMillis(timeLeft),
        secondLine = secondLine,
        timeLeft = timeLeft
    )
}
