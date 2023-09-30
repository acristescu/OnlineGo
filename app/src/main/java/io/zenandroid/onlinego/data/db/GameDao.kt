package io.zenandroid.onlinego.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.ChallengeNotification
import io.zenandroid.onlinego.data.model.local.ChatMetadata
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.model.local.GameNotificationWithDetails
import io.zenandroid.onlinego.data.model.local.HistoricGamesMetadata
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollectionSolutionMetadata
import io.zenandroid.onlinego.data.model.local.Score
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import kotlinx.coroutines.flow.Flow

private const val MAX_ALLOWED_SQL_PARAMS = 999

/**
 * Created by alex on 04/06/2018.
 */
@Dao
abstract class GameDao {

    @Query("SELECT id FROM game WHERE phase <> 'FINISHED' AND (white_id = :userId OR black_id = :userId)")
    abstract fun getActiveGameIds(userId: Long?) : List<Long>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT *
        FROM game 
        LEFT JOIN (
            SELECT 
                gameId, 
                COUNT(*) as messagesCount 
            FROM message 
            WHERE seen <> 1
                AND playerId <> :userId
            GROUP BY gameId
        ) message 
        ON message.gameId == game.id 
        WHERE 
            phase <> 'FINISHED' 
            AND (white_id = :userId OR black_id = :userId)
    """)
    abstract fun monitorActiveGamesWithNewMessagesCount(userId: Long?) : Flowable<List<Game>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT * 
        FROM game 
        LEFT JOIN (
            SELECT 
                gameId, 
                COUNT(*) as messagesCount 
            FROM message 
            WHERE seen <> 1 
                AND playerId <> :userId
            GROUP BY gameId
        ) message 
        ON message.gameId == game.id 
        WHERE 
            phase = 'FINISHED' 
            AND (white_id = :userId OR black_id = :userId)
            AND ended > (SELECT STRFTIME('%s','now','-3 days') * 1000000)
        ORDER BY ended DESC 
        LIMIT 25
        """)
    abstract fun monitorRecentGames(userId: Long?) : Flowable<List<Game>>

    @Query("""
        SELECT * 
        FROM game 
        WHERE 
            phase = 'FINISHED' 
            AND (white_id = :userId OR black_id = :userId)
            AND id NOT IN (
                SELECT id 
                FROM game
                WHERE 
                    phase = 'FINISHED' 
                    AND (white_id = :userId OR black_id = :userId)
                    AND ended > (SELECT STRFTIME('%s','now','-3 days') * 1000000)
                    ORDER BY ended DESC 
                    LIMIT 25
                )
        ORDER BY ended DESC 
        LIMIT 10
        """)
    abstract fun monitorFinishedNotRecentGames(userId: Long?) : Flowable<List<Game>>

    @Query("SELECT * FROM game WHERE phase = 'FINISHED' AND (white_id = :userId OR black_id = :userId) AND ended < :endedBefore ORDER BY ended DESC LIMIT 10")
    abstract fun monitorFinishedGamesEndedBefore(userId: Long?, endedBefore: Long) : Flowable<List<Game>>

    @Query("""
        SELECT id 
        FROM game 
        WHERE 
            id in (:ids) 
            AND phase = 'FINISHED' 
            AND outcome <> '' 
            AND outcome IS NOT NULL 
            AND blackLost IS NOT NULL 
            AND whiteLost IS NOT NULL
            AND ended IS NOT NULL
            """)
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
        val existingGames = games.map(Game::id).chunked(MAX_ALLOWED_SQL_PARAMS).flatMap(::getGameList)
        val newGames = games.filter { candidate ->
            existingGames.find { it.id == candidate.id } == null
        }
        FirebaseCrashlytics.getInstance().log("Inserting ${newGames.size} games out of ${games.size}")
        insertAllGamesInternal(newGames)

        val updatedGames = mutableListOf<Game>()
        for(oldGame in existingGames) {
            val updatedGame = games.find { it.id == oldGame.id }
            updatedGame?.let {
                updatedGames.add(it.copy(
                    blackPlayer = if(it.blackPlayer.country == null) oldGame.blackPlayer else it.blackPlayer,
                    whitePlayer = if(it.whitePlayer.country == null) oldGame.whitePlayer else it.whitePlayer,
                    moves = if(it.moves.isNullOrEmpty()) oldGame.moves else it.moves,
                    undoRequested = if(oldGame.undoRequested != null && it.undoRequested == null && oldGame.moves == it.moves) oldGame.undoRequested else it.undoRequested
                ))
            }
        }

        FirebaseCrashlytics.getInstance().log("Updating ${existingGames.size} games out of ${games.size}")
        updateGames(updatedGames)
    }

    @Transaction
    open fun insertHistoricGames(games: List<Game>, metadata: HistoricGamesMetadata) {
        insertAllGames(games)
        updateHistoricGameMetadata(metadata)
    }

    @Query("SELECT * FROM game WHERE id in (:ids)")
    abstract fun getGameList(ids: List<Long>): List<Game>

    @Update
    abstract fun update(game: Game)

    @Query("UPDATE game SET moves = :moves WHERE id = :id")
    abstract fun updateMovesInternal(id: Long, moves: List<Cell>)

    @Transaction
    open fun addMoveToGame(gameId: Long, moveNumber: Int, move: Cell) {
        //
        // Careful, moveNumber is 1-based not 0-based. Pascal FTW!!!
        //
        getGame(gameId).blockingGet().let { game ->
            game.moves?.let {
                val mutable = it.toMutableList()
                while(mutable.size < moveNumber) {
                    mutable.add(Cell(-1, -1))
                }
                mutable[moveNumber - 1] = move
                updateMovesInternal(gameId, mutable)
            }

        }
    }


    @Query("UPDATE game SET phase = :phase WHERE id = :id")
    abstract fun updatePhase(id: Long, phase: Phase)

    @Query("UPDATE game SET removedStones = :stones WHERE id = :id")
    abstract fun updateRemovedStones(id: Long, stones: String?)

    @Query("""
        UPDATE game 
        SET 
            white_acceptedStones = :white_stones, 
            black_acceptedstones= :black_stones 
        WHERE id = :id
        """)
    abstract fun updateRemovedStonesAccepted(id: Long, white_stones: String?, black_stones: String?)

    @Query("UPDATE game SET undoRequested = :moveNo WHERE id = :id")
    abstract fun updateUndoRequested(id: Long, moveNo: Int)

    @Transaction
    open fun updateUndoAccepted(id: Long, moveNo: Int) {
        getGame(id).blockingGet().let {
            update(it.copy(
                undoRequested = null,
                moves = if(it.moves?.size == moveNo) it.moves.dropLast(1).toMutableList() else it.moves
            ))
        }
    }

    @Transaction
    open fun updateClock(
            id: Long,
            playerToMoveId: Long?,
            clock: Clock?) {
        getGame(id).blockingGet().let {
            update(it.copy(
                undoRequested = if(it.playerToMoveId != playerToMoveId) null else it.undoRequested,
                playerToMoveId = playerToMoveId,
                clock = clock,
                pausedSince = when(clock?.newPausedState) {
                    true -> clock.newPausedSince
                    false -> null
                    else -> it.pausedSince
                }
            ))
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
            moves: List<Cell>,
            removedStones: String?,
            whiteScore: Score?,
            blackScore: Score?,
            clock: Clock?,
            blackLost: Boolean?,
            whiteLost: Boolean?,
            ended: Long?, // MICROSECONDS!!!
            undoRequested: Int?) {
        getGame(id).blockingGet().let {
            update(it.copy(
                outcome = outcome,
                phase = phase,
                playerToMoveId = playerToMoveId,
                initialState = initialState,
                whiteGoesFirst = whiteGoesFirst,
                moves = moves,
                removedStones = removedStones,
                whiteScore = whiteScore,
                blackScore = blackScore,
                clock = clock,
                undoRequested = undoRequested,
                blackLost = blackLost,
                whiteLost = whiteLost,
                ended = ended ?: it.ended
            ))
        }
    }

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun monitorGame(id: Long): Flowable<Game>

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun monitorGameFlow(id: Long): Flow<Game>

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun getGame(id: Long): Single<Game>

    @Query("SELECT * FROM game WHERE id = :id")
    abstract fun getGameMaybe(id: Long): Maybe<Game>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertMessage(message: Message)

    @Query("SELECT * FROM message WHERE gameId = :gameId ORDER BY date ASC")
    abstract fun getMessagesForGameRxJava(gameId: Long): Flowable<List<Message>>

    @Query("SELECT * FROM message WHERE gameId = :gameId ORDER BY date ASC")
    abstract fun getMessagesForGame(gameId: Long): Flow<List<Message>>

    @Query("SELECT chatId FROM message")
    abstract fun getAllMessageIDs(): Single<List<String>>

    @Query("UPDATE message SET seen = 1 WHERE chatId in (:ids)")
    abstract fun markMessagesAsRead(ids: List<String>)

    @Query("UPDATE message SET seen = 1 WHERE gameId = :gameId AND date <= :date")
    abstract fun markGameMessagesAsReadUpTo(gameId: Long, date: Long)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertChallengeNotifications(list: List<ChallengeNotification>)

    @Query("DELETE FROM challengeNotification")
    abstract fun deleteChallengeNotifications()

    @Transaction
    open fun replaceChallengeNotifications(list: List<ChallengeNotification>) {
        deleteChallengeNotifications()
        insertChallengeNotifications(list)
    }

    @Query ("SELECT * FROM challengeNotification")
    abstract fun getChallengeNotifications(): Flowable<List<ChallengeNotification>>

    @Transaction
    @Query ("SELECT * FROM gamenotification")
    abstract fun getGameNotifications(): Flowable<List<GameNotificationWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAllGameNotifications(list: List<GameNotification>)

    @Query("DELETE FROM gamenotification")
    abstract fun deleteGameNotifications()

    @Transaction
    open fun replaceGameNotifications(list: List<GameNotification>) {
        deleteGameNotifications()
        insertAllGameNotifications(list)
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT DISTINCT id, username, icon, rating, country FROM (
            SELECT 
                black_id as id,
                black_username as username,
                black_icon as icon,
                black_rating as rating,
                black_country as country,
                lastMove
            FROM game 
            WHERE black_id <> :userId 
            
            UNION 
            
            SELECT 
                white_id as id,
                white_username as username,
                white_icon as icon,
                white_rating as rating,
                white_country as country,
                lastMove
            FROM game
            WHERE white_id <> :userId
        ) 
        ORDER BY 
            lastMove DESC
        LIMIT 25
    """)
    abstract suspend fun getRecentOpponents(userId: Long?): List<Player>

    @Transaction
    open fun insertJosekiPositionsWithChildren(fullyLoadedPositions: List<JosekiPosition>, children: List<JosekiPosition>) {
        insertJosekiPositionsReplacingDuplicates(fullyLoadedPositions)
        deleteOldJosekiPosition(children.map { it.node_id!! }, children.map { it.play!! })
        insertJosekiPositionsIgnoringDuplicates(children)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertJosekiPositionsReplacingDuplicates(positions: List<JosekiPosition>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertJosekiPositionsIgnoringDuplicates(positions: List<JosekiPosition>)

    @Query("DELETE FROM josekiposition WHERE node_id NOT IN (:nodeIds) AND play IN (:play)")
    abstract fun deleteOldJosekiPosition(nodeIds: List<Long>, play: List<String>)

    @Query("SELECT * FROM josekiposition WHERE play = '.root'")
    abstract fun getJosekiRootPosition(): Flowable<JosekiPosition>

    @Query("SELECT * FROM josekiposition WHERE node_id = :posId AND play IS NOT NULL")
    abstract fun getJosekiPostion(posId: Long): Flowable<JosekiPosition>

    @Query("SELECT * FROM josekiposition WHERE parent_id = :parentId")
    abstract fun getChildrenPositions(parentId: Long): List<JosekiPosition>

    @Query("SELECT * FROM historicgamesmetadata WHERE id = 0")
    abstract fun monitorHistoricGameMetadata(): Flowable<HistoricGamesMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateHistoricGameMetadata(metadata: HistoricGamesMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateChatMetadata(metadata: ChatMetadata)

    @Query("SELECT * FROM ChatMetadata WHERE id = 0")
    abstract fun monitorChatMetadata(): Flowable<ChatMetadata>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertMessages(messages: List<Message>)

    @Transaction
    open fun insertMessagesFromRest(messages: List<Message>) {
        if(messages.isNotEmpty()) {
            insertMessages(messages)
            updateChatMetadata(ChatMetadata(0, messages.last().chatId))
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPuzzleCollections(collections: List<PuzzleCollection>)

    @Query("SELECT * FROM puzzlecollection")
    abstract fun getAllPuzzleCollections(): Flowable<List<PuzzleCollection>>

    @Query("SELECT count(*) FROM puzzlecollection")
    abstract fun getPuzzleCollectionCount(): Single<Int>

    @Query("SELECT * FROM puzzlecollection WHERE id = :collectionId")
    abstract fun getPuzzleCollection(collectionId: Long): Flowable<PuzzleCollection>

    @Query("SELECT * FROM puzzle WHERE puzzle_puzzle_collection = :collectionId")
    abstract fun getPuzzleCollectionPuzzles(collectionId: Long): Flowable<List<Puzzle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPuzzles(puzzles: List<Puzzle>)

    @Query("SELECT * FROM puzzle WHERE id = :puzzleId")
    abstract fun getPuzzle(puzzleId: Long): Flowable<Puzzle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPuzzleRating(rating: PuzzleRating)

    @Query("SELECT * FROM puzzlerating WHERE puzzleId = :puzzleId")
    abstract fun getPuzzleRating(puzzleId: Long): Flowable<PuzzleRating>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertPuzzleSolutions(solutions: List<PuzzleSolution>)

    @Query("SELECT * FROM puzzlesolution WHERE puzzle = :puzzleId")
    abstract fun getPuzzleSolution(puzzleId: Long): Flowable<List<PuzzleSolution>>

    @Insert
    abstract fun insertPuzzleCollectionVisit(visit: VisitedPuzzleCollection): Completable

    @Query("SELECT collectionId, max(timestamp) timestamp, sum(count) count FROM visitedpuzzlecollection" +
            " GROUP BY collectionId" +
            " ORDER BY max(timestamp) DESC")
    abstract fun getRecentPuzzleCollections(): Flowable<List<VisitedPuzzleCollection>>

    @Query("SELECT puzzle.puzzle_puzzle_collection collectionId, count(DISTINCT puzzlesolution.puzzle) count FROM puzzlesolution" +
            " INNER JOIN puzzle ON puzzle.id = puzzlesolution.puzzle" +
            " GROUP BY puzzle.puzzle_puzzle_collection")
    abstract fun getPuzzleCollectionSolutions(): Flowable<List<PuzzleCollectionSolutionMetadata>>
}
