package io.zenandroid.onlinego.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import io.zenandroid.onlinego.model.local.DbGame

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
interface GameDao {

    @Query("SELECT * FROM dbgame WHERE phase <> 'FINISHED' AND (white_id = :userId OR black_id = :userId)")
    fun monitorActiveGames(userId: Long?) : Flowable<List<DbGame>>

    @Query("SELECT * FROM dbgame WHERE phase = 'FINISHED' AND (white_id = :userId OR black_id = :userId) ORDER BY ended DESC")
    fun monitorHistoricGames(userId: Long?) : Flowable<List<DbGame>>

    @Query("SELECT id FROM dbgame WHERE id in (:ids) AND phase = 'FINISHED' AND outcome <> ''")
    fun getHistoricGamesThatDontNeedUpdating(ids: List<Long>) : List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(games: List<DbGame>)

    @Update()
    fun update(game: DbGame)

    @Query("SELECT * FROM dbgame WHERE id = :id")
    fun monitorGame(id: Long): Flowable<DbGame>

    @Query("SELECT * FROM dbgame WHERE id = :id")
    fun getGame(id: Long): Single<DbGame>
}