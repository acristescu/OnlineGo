package io.zenandroid.onlinego.utils.moshiadapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType

class HashMapOfCellToStoneTypeMoshiAdapter {
    @ToJson
    fun toJson(hash: HashMap<Cell, StoneType>): String? {
        val sb = StringBuffer()
        for( (p, type) in hash) {
            sb.append("${p.x},${p.y},$type ")
        }
        return sb.removeSuffix(" ").toString()
    }

    @FromJson
    fun fromJson(json: String): HashMap<Cell, StoneType>? {
        val retval = HashMap<Cell, StoneType>()
        json.split(' ').forEach {
            if(it.isNotEmpty()) {
                val tokens = it.split(',')
                retval[Cell(tokens[0].toInt(), tokens[1].toInt())] = StoneType.valueOf(tokens[2])
            }
        }
        return retval
    }
}