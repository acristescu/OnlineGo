package io.zenandroid.onlinego.utils

import org.json.JSONArray
import org.json.JSONObject

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
            Math.log(Math.min(MAX_RATING, Math.max(MIN_RATING, it)) / 850.0) / 0.032
        }

fun formatRank(rank: Double?) =
    when(rank) {
        null -> "?"
        in 0 .. 29 -> "${(30 - rank).toInt()}k"
        in 30 .. 100 -> "${(rank - 29).toInt()}d"
        else -> "???"
    }

fun convertCountryCodeToEmojiFlag(country: String):String {
    val c1 = '\uDDE6' + country[0].minus('a')
    val c2 = '\uDDE6' + country[1].minus('a')
    return "\uD83C$c1\uD83C$c2"
}



