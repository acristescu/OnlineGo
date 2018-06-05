package io.zenandroid.onlinego.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import io.zenandroid.onlinego.model.local.DbGame

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
interface GameDao {

    @Query("SELECT * FROM dbgame")
    fun getActiveGames() : Flowable<List<DbGame>>

    @Query("SELECT * FROM dbgame")
    fun getFinishedGames() : Flowable<List<DbGame>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(games: List<DbGame>)
}