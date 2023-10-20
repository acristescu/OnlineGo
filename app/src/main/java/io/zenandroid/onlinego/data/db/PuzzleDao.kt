package io.zenandroid.onlinego.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.PuzzleCollectionSolutionMetadata
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PuzzleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPuzzleCollections(collections: List<PuzzleCollection>)

    @Query("SELECT * FROM puzzlecollection")
    abstract fun getAllPuzzleCollections(): Flow<List<PuzzleCollection>>

    @Query("SELECT count(*) FROM puzzlecollection")
    abstract suspend fun getPuzzleCollectionCount(): Int

    @Query("SELECT * FROM puzzlecollection WHERE id = :collectionId")
    abstract fun getPuzzleCollection(collectionId: Long): Flow<PuzzleCollection>

    @Query("SELECT * FROM puzzle WHERE puzzle_puzzle_collection = :collectionId")
    abstract fun getPuzzleCollectionPuzzles(collectionId: Long): Flow<List<Puzzle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPuzzles(puzzles: List<Puzzle>)

    @Query("SELECT * FROM puzzle WHERE id = :puzzleId")
    abstract fun getPuzzle(puzzleId: Long): Flow<Puzzle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPuzzleRating(rating: PuzzleRating)

    @Query("SELECT * FROM puzzlerating WHERE puzzleId = :puzzleId")
    abstract fun getPuzzleRating(puzzleId: Long): Flow<PuzzleRating>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPuzzleSolutions(solutions: List<PuzzleSolution>)

    @Query("SELECT * FROM puzzlesolution WHERE puzzle = :puzzleId")
    abstract fun getPuzzleSolution(puzzleId: Long): Flow<List<PuzzleSolution>>

    @Insert
    abstract suspend fun insertPuzzleCollectionVisit(visit: VisitedPuzzleCollection)

    @Query("SELECT collectionId, max(timestamp) timestamp, sum(count) count FROM visitedpuzzlecollection" +
            " GROUP BY collectionId" +
            " ORDER BY max(timestamp) DESC")
    abstract fun getRecentPuzzleCollections(): Flow<List<VisitedPuzzleCollection>>

    @Query("SELECT puzzle.puzzle_puzzle_collection collectionId, count(DISTINCT puzzlesolution.puzzle) count FROM puzzlesolution" +
            " INNER JOIN puzzle ON puzzle.id = puzzlesolution.puzzle" +
            " GROUP BY puzzle.puzzle_puzzle_collection")
    abstract fun getPuzzleCollectionSolutions(): Flow<List<PuzzleCollectionSolutionMetadata>>

    @Query("SELECT puzzle.id FROM puzzle" +
            " LEFT JOIN puzzlesolution ON puzzlesolution.puzzle = puzzle.id" +
            " WHERE puzzle_puzzle_collection = :collectionId" +
            " GROUP BY puzzlesolution.puzzle" +
            " HAVING COUNT(puzzlesolution.id) == 0" +
            " LIMIT 1")
    abstract suspend fun getPuzzleCollectionFirstUnsolved(collectionId: Long): Long?

}
