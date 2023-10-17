package io.zenandroid.onlinego.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import java.time.Instant

/**
 * Created by alex on 07/06/2018.
 */
class DbTypeConverters {

    companion object {

        @TypeConverter
        @JvmStatic
        fun stringToListOfCell(s: String): List<Cell>? {
            if(s.isEmpty()) {
                return mutableListOf()
            }
            val list = mutableListOf<Cell>()
            for( i in s.indices step 2) {
                list += Cell(s[i] - 'a', s[i+1] - 'a')
            }
            return list
        }

        @TypeConverter
        @JvmStatic
        fun listOfCellToString(list: List<Cell>?): String {
            val buf = StringBuffer((list?.size ?:0) * 2)
            list?.forEach {
                buf.append('a' + it.x )
                buf.append('a' + it.y )
            }
            return buf.toString()
        }

        @TypeConverter
        @JvmStatic
        fun playCategoryToString(playCategory: PlayCategory?) = playCategory?.toString()

        @TypeConverter
        @JvmStatic
        fun stringToPlayCategory(playCategory: String?) = playCategory?.let(PlayCategory::valueOf)

        @TypeConverter
        @JvmStatic
        fun messageTypeToString(type: Message.Type?) = type?.toString()

        @TypeConverter
        @JvmStatic
        fun stringToMessageType(type: String?) = type?.let(Message.Type::valueOf)

        @TypeConverter
        @JvmStatic
        fun phaseToString(phase: Phase?) = phase?.toString()

        @TypeConverter
        @JvmStatic
        fun stringToPhase(phase: String?) = phase?.let(Phase::valueOf)

        @TypeConverter
        @JvmStatic
        fun instantToLong(instant: Instant?) = instant?.let(Instant::toEpochMilli)

        @TypeConverter
        @JvmStatic
        fun longToInstant(instant: Long?) = instant?.let(Instant::ofEpochMilli)

        val moveTreeAdapter = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(MoveTree::class.java)

        @TypeConverter
        @JvmStatic
        fun moveTreeToString(moveTree: MoveTree?) = moveTree?.let(moveTreeAdapter::toJson)

        @TypeConverter
        @JvmStatic
        fun stringToMoveTree(moveTree: String?) = moveTree?.let(moveTreeAdapter::fromJson)
    }
}