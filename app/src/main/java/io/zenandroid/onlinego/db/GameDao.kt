package io.zenandroid.onlinego.db

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single
import io.zenandroid.onlinego.model.local.*
import io.zenandroid.onlinego.model.ogs.OGSPlayer
import io.zenandroid.onlinego.model.ogs.Phase

/**
 * Created by 44108952 on 04/06/2018.
 */
@Dao
abstract class GameDao {

    @Query("SELECT * FROM game WHERE phase <> 'FINISHED' AND (white_id = :userId OR black_id = :userId)")
    abstract fun monitorActiveGames(userId: Long?) : Flowable<List<Game>>

    @Query("SELECT * FROM game WHERE phase = 'FINISHED' AND (white_id = :userId OR black_id = :userId) ORDER BY ended DESC LIMIT 25")
    abstract fun monitorHistoricGames(userId: Long?) : Flowable<List<Game>>

    @Query("SELECT id FROM game WHERE id in (:ids) AND phase = 'FINISHED' AND outcome <> ''")
    abstract fun getHistoricGamesThatDontNeedUpdating(ids: List<Long>) : List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertAllGamesInternal(games: List<Game>)

    @Update
    abstract fun updateGames(games: List<Game>)

    //
    // All of this complication is needed because sometimes the backend responds with
    // incomplete data for the players and we don't want to overwrite good data with bad
    // e.g. icon missing when getting stuff through the /overview call
    //
    @Transaction
    open fun insertAllGames(games: List<Game>) {
        val existingGames = getGameList(games.map(Game::id))
        val newGames = games.filter { candidate ->
            existingGames.find { it.id == candidate.id } == null
        }
        insertAllGamesInternal(newGames)

        val updatedGames = mutableListOf<Game>()
        for(oldGame in existingGames) {
            val updatedGame = games.find { it.id == oldGame.id }
            updatedGame?.let {
                if(it.blackPlayer.country == null) {
                    it.blackPlayer = oldGame.blackPlayer
                }
                if(it.whitePlayer.country == null) {
                    it.whitePlayer = oldGame.whitePlayer
                }
                updatedGames.add(it)
            }
        }

        updateGames(updatedGames)
    }

    @Query("SELECT * FROM game WHERE id in (:ids)")
    abstract fun getGameList(ids: List<Long>): List<Game>

    @Update
    abstract fun update(game: Game)

    @Query("UPDATE game SET moves = :moves WHERE id = :id")
    abstract fun updateMoves(id: Long, moves: MutableList<MutableList<Int>>)

    @Query("UPDATE game SET phase = :phase WHERE id = :id")
    abstract fun updatePhase(id: Long, phase: Phase)

    @Query("UPDATE game SET removedStones = :stones WHERE id = :id")
    abstract fun updateRemovedStones(id: Long, stones: String?)

    @Query("UPDATE game SET undoRequested = :moveNo WHERE id = :id")
    abstract fun updateUndoRequested(id: Long, moveNo: Int)

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
            clock: Clock?,
            undoRequested: Int?) {
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
            it.undoRequested = undoRequested
            update(it)
        }
    }

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun monitorGame(id: Long): Flowable<Game>

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun getGame(id: Long): Single<Game>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertMessage(message: Message)

    @Query("SELECT * FROM message WHERE gameId = :gameId")
    abstract fun getMessagesForGame(gameId: Long): Flowable<List<Message>>

    @Query("UPDATE message SET seen = 1 WHERE chatId in (:ids)")
    abstract fun markMessagesAsRead(ids: List<String>)

    @Query("DELETE FROM challenge")
    abstract fun deleteAllChallenges()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllChallenges(list: List<Challenge>)

    @Transaction
    open fun replaceAllChallenges(list: List<Challenge>) {
        deleteAllChallenges()
        insertAllChallenges(list)
    }

    @Query ("SELECT * FROM challenge")
    abstract fun getChallenges(): Flowable<List<Challenge>>

    @Query ("SELECT * FROM gamenotification")
    abstract fun getGameNotifications(): Flowable<List<GameNotificationWithDetails>>

    @Query("SELECT id FROM game WHERE id not in (:ids) AND phase <> 'FINISHED'")
    abstract fun getGamesThatShouldBeFinished(ids: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllGameNotifications(list: List<GameNotification>)

    @Query("DELETE FROM gamenotification")
    abstract fun deleteGameNotifications()

    @Transaction
    open fun replaceGameNotifications(list: List<GameNotification>) {
        deleteGameNotifications()
        insertAllGameNotifications(list)
    }

    @Query("""
        SELECT 
            black_id as id,
            black_username as username,
            black_icon as icon,
            black_rating as rating,
            black_country as country
        FROM game 
        WHERE black_id <> :userId 
        
        UNION 
        
        SELECT 
            white_id as id,
            white_username as username,
            white_icon as icon,
            white_rating as rating,
            white_country as country
        FROM game
        WHERE white_id <> :userId
    """)
    abstract fun getRecentOpponents(userId: Long?): Single<List<Player>>
}