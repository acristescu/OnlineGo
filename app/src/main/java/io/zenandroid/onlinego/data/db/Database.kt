package io.zenandroid.onlinego.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.zenandroid.onlinego.data.model.local.*
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition

/**
 * Created by alex on 04/06/2018.
 */
@Database(
        entities = [Game::class, Message::class, Challenge::class, GameNotification::class, JosekiPosition::class, HistoricGamesMetadata::class, ChatMetadata::class, ChallengeNotification::class],
        version = 18,
        exportSchema = true,
        autoMigrations = [
            AutoMigration (from = 16, to = 17),
            AutoMigration (from = 17, to = 18),
        ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Database: RoomDatabase() {
    abstract fun gameDao(): GameDao
}