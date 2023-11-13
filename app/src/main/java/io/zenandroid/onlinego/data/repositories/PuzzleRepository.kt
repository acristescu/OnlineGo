package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.zenandroid.onlinego.data.db.PuzzleDao
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant.now

class PuzzleRepository(
  private val restService: OGSRestService,
  private val dao: PuzzleDao
) {

  private val refreshCooldownSeconds = 60 * 60 * 24

  suspend fun fetchAllPuzzleCollections() {
    withContext(Dispatchers.Default) {
      val currentPuzzleCount = dao.getPuzzleCollectionCount()
      val lastRefresh = now().epochSecond - PersistenceManager.puzzleCollectionLastRefresh
      if(lastRefresh > refreshCooldownSeconds || currentPuzzleCount < 1) {
        restService.getPuzzleCollections()
          .catch { onError(it) }
          .collect { saveCollectionsToDB(it) }

        PersistenceManager.puzzleCollectionLastRefresh = now().epochSecond
      }
    }
  }

  fun observeAllPuzzleCollections(): Flow<List<PuzzleCollection>> {
    return dao.getAllPuzzleCollections()
      .distinctUntilChanged()
  }

  fun getRecentPuzzleCollections(): Flow<List<VisitedPuzzleCollection>> {
    return dao.getRecentPuzzleCollections()
      .distinctUntilChanged()
  }

  suspend fun markPuzzleCollectionVisited(id: Long) {
    dao.insertPuzzleCollectionVisit(VisitedPuzzleCollection(id))
  }

  suspend fun fetchPuzzleCollection(id: Long) {
    withContext(Dispatchers.IO) {
      val collection = restService.getPuzzleCollection(id)
      saveCollectionsToDB(listOf(collection))
      val puzzles = restService.getPuzzleCollectionContents(id)
      savePuzzlesToDB(puzzles)
    }
  }

  fun observePuzzleCollection(id: Long): Flow<PuzzleCollection> {
    return dao.getPuzzleCollection(id)
      .distinctUntilChanged()
  }

  fun observePuzzleCollectionContents(id: Long): Flow<List<Puzzle>> {
    return dao.getPuzzleCollectionPuzzles(id)
      .distinctUntilChanged()
  }

  suspend fun fetchPuzzle(id: Long) {
    withContext(Dispatchers.IO) {
      val puzzle = restService.getPuzzle(id)
      savePuzzlesToDB(listOf(puzzle))
      val rating = restService.getPuzzleRating(id)
      savePuzzleRatingToDB(rating.copy(puzzleId = id))
      val solutions = restService.getPuzzleSolutions(id)
      savePuzzleSolutionsToDB(solutions)
    }
  }

  fun observePuzzle(id: Long): Flow<Puzzle> {
    return dao.getPuzzle(id)
      .distinctUntilChanged()
  }

  suspend fun fetchPuzzleRating(id: Long) {
    withContext(Dispatchers.IO) {
      val rating = restService.getPuzzleRating(id)
      savePuzzleRatingToDB(rating.copy(puzzleId = id))
    }
  }

  fun observePuzzleRating(id: Long): Flow<PuzzleRating> {
    return dao.getPuzzleRating(id)
      .catch {
        emit(
          PuzzleRating(
            puzzleId = id,
            rating = -1,
          )
        )
      }
      .distinctUntilChanged()
  }

  suspend fun ratePuzzle(id: Long, rating: Int): Flow<PuzzleRating> {
    val puzzleRating = PuzzleRating(puzzleId = id, rating = rating)
    restService.ratePuzzle(id, puzzleRating)
    savePuzzleRatingToDB(puzzleRating)
    return flowOf(puzzleRating)
  }

  suspend fun fetchPuzzleSolutions(id: Long) {
    withContext(Dispatchers.IO) {
      val solutions = restService.getPuzzleSolutions(id)
      savePuzzleSolutionsToDB(solutions)
    }
  }

  fun observePuzzleSolutions(id: Long): Flow<List<PuzzleSolution>> {
    return dao.getPuzzleSolution(id)
      .catch { emit(emptyList()) }
      .distinctUntilChanged()
  }

  fun getPuzzleCollectionSolutions(): Flow<Map<Long, Int>> {
    return dao.getPuzzleCollectionSolutions()
      .catch { emit(emptyList()) }
      .map { it.sortedBy { it.collectionId } }
      .distinctUntilChanged()
      .map { it.associateBy({ it.collectionId }, { it.count }) }
  }

  suspend fun getPuzzleCollectionFirstUnsolved(id: Long): Long? {
    return dao.getPuzzleCollectionFirstUnsolved(id)
  }

  suspend fun markPuzzleSolved(id: Long, record: PuzzleSolution) {
    restService.markPuzzleSolved(id, record)
    savePuzzleSolutionsToDB(listOf(record))
  }

  private suspend fun saveCollectionsToDB(list: List<PuzzleCollection>) {
    dao.insertPuzzleCollections(list)
  }

  private suspend fun savePuzzlesToDB(list: List<Puzzle>) {
    dao.insertPuzzles(list)
  }

  private suspend fun savePuzzleRatingToDB(item: PuzzleRating) {
    dao.insertPuzzleRating(item)
  }

  private suspend fun savePuzzleSolutionsToDB(list: List<PuzzleSolution>) {
    dao.insertPuzzleSolutions(list)
  }

  private fun onError(error: Throwable) {
    Log.e("PuzzleRepository", error.message, error)
    recordException(error)
  }
}
