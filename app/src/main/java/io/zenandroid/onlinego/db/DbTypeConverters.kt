package io.zenandroid.onlinego.db

import android.arch.persistence.room.TypeConverter
import io.zenandroid.onlinego.model.local.Message
import io.zenandroid.onlinego.model.ogs.Phase

/**
 * Created by 44108952 on 07/06/2018.
 */
class DbTypeConverters {

    companion object {

        @TypeConverter
        @JvmStatic
        fun listOfListOfIntToString(list: MutableList<MutableList<Int>>?): String {
            val buf = StringBuffer()
            list?.forEach {
                it.forEachIndexed { index, value ->
                    if(index % 3 != 2) {
                        buf.append(value.toString())
                        buf.append(',')
                    }
                }
            }
            if (buf.endsWith(',')) {
                buf.deleteCharAt(buf.lastIndex)
            }
            return buf.toString()
        }

        @TypeConverter
        @JvmStatic
        fun stringToListOfListOfInt(s: String): MutableList<MutableList<Int>>? {
            if(s.isEmpty()) {
                return null
            }
            val list = mutableListOf<MutableList<Int>>()
            s.split(',').map { it.toInt() }.forEachIndexed { index, value ->
                if (index % 2 == 0) {
                    list += mutableListOf(value)
                } else {
                    list.last().add(value)
                }
            }
            return list
        }

        @TypeConverter
        @JvmStatic
        fun phaseToString(phase: Phase?) = phase?.toString()

        @TypeConverter
        @JvmStatic
        fun stringToPhase(phase: String?) = phase?.let(Phase::valueOf)

        @TypeConverter
        @JvmStatic
        fun messageTypeToString(type: Message.Type?) = type?.toString()

        @TypeConverter
        @JvmStatic
        fun stringToMessageType(type: String?) = type?.let(Message.Type::valueOf)
    }
}