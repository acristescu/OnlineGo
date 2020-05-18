package io.zenandroid.onlinego.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.zenandroid.onlinego.model.local.*
import io.zenandroid.onlinego.model.ogs.JosekiPosition

/**
 * Created by alex on 04/06/2018.
 */
@Database(
        entities = [Game::class, Message::class, Challenge::class, GameNotification::class, JosekiPosition::class, HistoricGamesMetadata::class],
        version = 9
)
@TypeConverters(DbTypeConverters::class)
abstract class Database: RoomDatabase() {
    abstract fun gameDao(): GameDao
}