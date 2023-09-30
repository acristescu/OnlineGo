package io.zenandroid.onlinego.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.zenandroid.onlinego.data.model.local.*
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution

/**
 * Created by alex on 04/06/2018.
 */
@Database(
        entities = [
            Game::class,
            Message::class,
            Challenge::class,
            GameNotification::class,
            JosekiPosition::class,
            HistoricGamesMetadata::class,
            ChatMetadata::class,
            ChallengeNotification::class,
            PuzzleCollection::class,
            Puzzle::class,
            PuzzleRating::class,
            PuzzleSolution::class,
            VisitedPuzzleCollection::class
        ],
        version = 19,
        exportSchema = true,
        autoMigrations = [
            AutoMigration (from = 16, to = 17),
            AutoMigration (from = 17, to = 18),
            AutoMigration (from = 18, to = 19),
        ]
)
@TypeConverters(DbTypeConverters::class)
abstract class Database: RoomDatabase() {
    abstract fun gameDao(): GameDao
}