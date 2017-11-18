package io.zenandroid.onlinego.utils

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
        else -> "???"
    }

fun convertCountryCodeToEmojiFlag(country: String?): String {
    if(country == null || country.length != 2) {
        return "\uD83C\uDDFA\uD83C\uDDF3"
    }
    val c1 = '\uDDE6' + country[0].minus('a')
    val c2 = '\uDDE6' + country[1].minus('a')
    return "\uD83C$c1\uD83C$c2"
}



