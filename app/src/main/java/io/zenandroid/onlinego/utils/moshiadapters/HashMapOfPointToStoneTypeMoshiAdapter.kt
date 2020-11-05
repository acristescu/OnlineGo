package io.zenandroid.onlinego.utils.moshiadapters

import android.graphics.Point
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.zenandroid.onlinego.data.model.StoneType

class HashMapOfPointToStoneTypeMoshiAdapter {
    @ToJson
    fun toJson(hash: HashMap<Point, StoneType>): String? {
        val sb = StringBuffer()
        for( (p, type) in hash) {
            sb.append("${p.x},${p.y},$type ")
        }
        return sb.removeSuffix(" ").toString()
    }

    @FromJson
    fun fromJson(json: String): HashMap<Point, StoneType>? {
        val retval = HashMap<Point, StoneType>()
        json.split(' ').forEach {
            if(it.isNotEmpty()) {
                val tokens = it.split(',')
                retval[Point(tokens[0].toInt(), tokens[1].toInt())] = StoneType.valueOf(tokens[2])
            }
        }
        return retval
    }
}