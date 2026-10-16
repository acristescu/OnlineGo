package io.zenandroid.onlinego.data.db

import androidx.room.TypeConverter
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.utils.appJson
import kotlin.time.Instant

/**
 * Created by alex on 07/06/2018.
 */
class DbTypeConverters {

    @TypeConverter
    fun stringToListOfCell(s: String): List<Cell>? {
        if (s.isEmpty()) {
            return mutableListOf()
        }
        val list = mutableListOf<Cell>()
        for (i in s.indices step 2) {
            list += Cell(s[i] - 'a', s[i + 1] - 'a')
        }
        return list
    }

    @TypeConverter
    fun listOfCellToString(list: List<Cell>?): String = buildString((list?.size ?: 0) * 2) {
        list?.forEach {
            append('a' + it.x)
            append('a' + it.y)
        }
    }

    @TypeConverter
    fun playCategoryToString(playCategory: PlayCategory?) = playCategory?.toString()

    @TypeConverter
    fun stringToPlayCategory(playCategory: String?) = playCategory?.let(PlayCategory::valueOf)

    @TypeConverter
    fun messageTypeToString(type: Message.Type?) = type?.toString()

    @TypeConverter
    fun stringToMessageType(type: String?) = type?.let(Message.Type::valueOf)

    @TypeConverter
    fun phaseToString(phase: Phase?) = phase?.toString()

    @TypeConverter
    fun stringToPhase(phase: String?) = phase?.let(Phase::valueOf)

    @TypeConverter
    fun instantToLong(instant: Instant?) = instant?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(instant: Long?) = instant?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun moveTreeToString(moveTree: MoveTree?) = moveTree?.let { appJson.encodeToString(it) }

    @TypeConverter
    fun stringToMoveTree(moveTree: String?) =
        moveTree?.let { appJson.decodeFromString<MoveTree>(it) }
}
