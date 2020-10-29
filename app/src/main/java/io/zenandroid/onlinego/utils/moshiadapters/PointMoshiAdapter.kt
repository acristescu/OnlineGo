package io.zenandroid.onlinego.utils.moshiadapters

import android.graphics.Point
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson


class PointMoshiAdapter {
    @ToJson
    fun toJson(p: Point): String? {
        return "${p.x},${p.y}"
    }

    @FromJson
    fun fromJson(json: String):Point? {
        val coords = json.split(',')
        if(coords.size != 2) throw JsonDataException("Cannot deserialize point: `$json`");
        return Point(coords[0].toInt(), coords[1].toInt())
    }
}