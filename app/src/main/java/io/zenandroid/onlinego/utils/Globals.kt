package io.zenandroid.onlinego.utils

import android.os.Build
import android.util.Log
import io.zenandroid.onlinego.game.GamePresenter
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.ogs.Time
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Math.ceil
import java.lang.Math.log

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

fun convertCountryCodeToEmojiFlag(country: String?): String {
    if(country == null || country.length != 2 || "un" == country) {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) ""
        else "\uD83C\uDDFA\uD83C\uDDF3"
    }
    val c1 = '\uDDE6' + country[0].minus('a')
    val c2 = '\uDDE6' + country[1].minus('a')
    return "\uD83C$c1\uD83C$c2"
}

fun computeTimeLeft(clock: Clock, playerTimeAny: Any, currentPlayer: Boolean): GamePresenter.TimerDetails {
    val timer = GamePresenter.TimerDetails()

    val now = System.currentTimeMillis()
    if(clock.receivedAt == 0L) {
        clock.receivedAt = now
    }
    val nowDelta = clock.receivedAt - clock.now
    val baseTime = clock.last_move + nowDelta
    var timeLeft = 0L
    if(playerTimeAny is Long) {
        // Simple timer
        timeLeft = playerTimeAny - if(currentPlayer) now else baseTime
    } else if (playerTimeAny is Map<*, *>) {

        val playerTime = Time.fromMap(playerTimeAny)
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
                val periodOffset = Math.floor((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

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
        Log.e("GamePresenter", "Unknown clock object $playerTimeAny")
    }

    timer.expired = timeLeft <= 0
    timer.firstLine = formatMillis(timeLeft)
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

fun plural(number: Long) = if(number > 1) "s" else ""


