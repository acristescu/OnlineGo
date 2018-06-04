package io.zenandroid.onlinego.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import io.reactivex.Flowable
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
interface GameDao {

    @Query("SELECT * FROM game WHERE phase IS NOT 'finished'")
    fun getActiveGames() : Flowable<List<Game>>

    @Query("SELECT * FROM game WHERE phase IS 'finished'")
    fun getFinishedGames() : Flowable<List<Game>>

    @Insert
    fun insertAll(games: List<Game>)
}