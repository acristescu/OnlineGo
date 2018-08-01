package io.zenandroid.onlinego.db

import android.arch.persistence.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import io.zenandroid.onlinego.model.local.Clock
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.local.InitialState
import io.zenandroid.onlinego.model.local.Score
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.RemovedStones

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
abstract class GameDao {

    @Query("SELECT * FROM game WHERE phase <> 'FINISHED' AND (white_id = :userId OR black_id = :userId)")
    abstract fun monitorActiveGames(userId: Long?) : Flowable<List<Game>>

    @Query("SELECT * FROM game WHERE phase = 'FINISHED' AND (white_id = :userId OR black_id = :userId) ORDER BY ended DESC")
    abstract fun monitorHistoricGames(userId: Long?) : Flowable<List<Game>>

    @Query("SELECT id FROM game WHERE id in (:ids) AND phase = 'FINISHED' AND outcome <> ''")
    abstract fun getHistoricGamesThatDontNeedUpdating(ids: List<Long>) : List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(games: List<Game>)

    @Update()
    abstract fun update(game: Game)

    @Query("UPDATE game SET moves = :moves WHERE id = :id")
    abstract fun updateMoves(id: Long, moves: MutableList<MutableList<Int>>)

    @Query("UPDATE game SET phase = :phase WHERE id = :id")
    abstract fun updatePhase(id: Long, phase: Phase)

    @Query("UPDATE game SET removedStones = :stones WHERE id = :id")
    abstract fun updateRemovedStones(id: Long, stones: String?)

    @Transaction
    open fun updateClock(
            id: Long,
            playerToMoveId: Long?,
            clock: Clock?) {
        getGame(id).blockingGet().let {
            it.playerToMoveId = playerToMoveId
            it.clock = clock
            update(it)
        }
    }

    @Transaction
    open fun updateGameData(
            id: Long,
            outcome: String?,
            phase: Phase,
            playerToMoveId: Long?,
            initialState: InitialState?,
            whiteGoesFirst: Boolean?,
            moves: MutableList<MutableList<Int>>,
            removedStones: String?,
            whiteScore: Score?,
            blackScore: Score?,
            clock: Clock?) {
        getGame(id).blockingGet().let {
            it.outcome = outcome
            it.phase = phase
            it.playerToMoveId = playerToMoveId
            it.initialState = initialState
            it.whiteGoesFirst = whiteGoesFirst
            it.moves = moves
            it.removedStones = removedStones
            it.whiteScore = whiteScore
            it.blackScore = blackScore
            it.clock = clock
            update(it)
        }
    }

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun monitorGame(id: Long): Flowable<Game>

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun getGame(id: Long): Single<Game>
}