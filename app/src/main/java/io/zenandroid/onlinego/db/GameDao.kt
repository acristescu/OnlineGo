package io.zenandroid.onlinego.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.zenandroid.onlinego.model.local.DbGame

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
interface GameDao {

    @Query("SELECT * FROM dbgame WHERE phase <> 'FINISHED'")
    fun getActiveGames() : Flowable<List<DbGame>>

    @Query("SELECT * FROM dbgame WHERE phase = 'FINISHED'")
    fun getFinishedGames() : Flowable<List<DbGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(games: List<DbGame>)

    @Update()
    fun update(game: DbGame)
}