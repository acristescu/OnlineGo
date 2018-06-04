package io.zenandroid.onlinego.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by 44108952 on 04/06/2018.
 */
@Database(
        entities = [Game::class],
        version = 1
)
abstract class Database: RoomDatabase() {
    abstract fun gameDao(): GameDao
}