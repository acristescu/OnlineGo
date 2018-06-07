package io.zenandroid.onlinego.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import io.zenandroid.onlinego.model.local.DbGame

/**
 * Created by 44108952 on 04/06/2018.
 */
@Database(
        entities = [DbGame::class],
        version = 1
)
@TypeConverters(DbTypeConverters::class)
abstract class Database: RoomDatabase() {
    abstract fun gameDao(): GameDao
}