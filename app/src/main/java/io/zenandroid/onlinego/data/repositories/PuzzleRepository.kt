package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.db.PuzzleDao
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import java.time.Instant.now
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PuzzleRepository(
    private val restService: OGSRestService,
    private val dao: PuzzleDao
) {

    private val refreshCooldownSeconds = 60 * 60 * 24

    private val disposable = CompositeDisposable()
    private val customMarkPattern = "<(.):([A-H]|[J-T]\\d{1,2})>".toPattern()
    private val headerWithMissingSpaceRegex = "#(?!\\s|#)".toRegex()

    fun getAllPuzzleCollections(): Flow<List<PuzzleCollection>> {
        val lastRefresh = now().getEpochSecond() - PersistenceManager.puzzleCollectionLastRefresh

        flow<Int> { dao.getPuzzleCollectionCount() }
            .flowOn(Dispatchers.IO)
            .onEach {
                if (lastRefresh > refreshCooldownSeconds || it < 1) {
                    restService.getPuzzleCollections()
                        .toObservable()
                        .asFlow()
                        .catch { onError(it) }
                        .onEach { saveCollectionsToDB(it) }
                        .single()

                    PersistenceManager.puzzleCollectionLastRefresh = now().getEpochSecond()
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        return dao.getAllPuzzleCollections()
                .onEach { it.forEach { c -> Log.d("PuzzleRepository", c.toString()) } }
                .distinctUntilChanged()
    }

    fun getRecentPuzzleCollections(): Flow<List<VisitedPuzzleCollection>> {
        return dao.getRecentPuzzleCollections()
                .onEach { it.forEach{ c -> Log.d("PuzzleRepository", c.toString()) } }
                .distinctUntilChanged()
    }

    fun markPuzzleCollectionVisited(id: Long): Flow<VisitedPuzzleCollection> {
        val visit = VisitedPuzzleCollection(id)

        return flow {
                    dao.insertPuzzleCollectionVisit(visit)
                    emit(visit)
                }
                .onEach { Log.d("PuzzleRepository", it.toString()) }
    }

    fun getPuzzleCollection(id: Long): Flow<PuzzleCollection> {
        disposable += restService.getPuzzleCollection(id).map(::listOf)
                .subscribe({ runBlocking { saveCollectionsToDB(it) } }, this::onError)

        return dao.getPuzzleCollection(id)
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleCollectionContents(id: Long): Flow<List<Puzzle>> {
        disposable += restService.getPuzzleCollectionContents(id)
                .subscribe({ runBlocking { savePuzzlesToDB(it) } }, this::onError)

        return dao.getPuzzleCollectionPuzzles(id)
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzle(id: Long): Flow<Puzzle> {
        disposable += restService.getPuzzle(id).map(::listOf)
                .subscribe({ runBlocking { savePuzzlesToDB(it) } }, this::onError)
        disposable += restService.getPuzzleRating(id)
                .subscribe({ runBlocking { savePuzzleRatingToDB(it) } }, this::onError)
        disposable += restService.getPuzzleSolutions(id)
                .subscribe({ runBlocking { savePuzzleSolutionsToDB(it) } }, this::onError)

        return dao.getPuzzle(id)
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleRating(id: Long): Flow<PuzzleRating> {
        disposable += restService.getPuzzleRating(id)
                .map { it.copy(puzzleId = id) }
                .subscribe({ runBlocking { savePuzzleRatingToDB(it) } }, this::onError)

        return dao.getPuzzleRating(id)
                .catch {
                    emit(PuzzleRating(
                        puzzleId = id,
                        rating = -1,
                    ))
                }
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun ratePuzzle(id: Long, rating: Int): Flow<PuzzleRating> =
        restService.ratePuzzle(id, rating)
                .doOnComplete { Log.d("PuzzleRepository", "rate $id: $rating") }
                .andThen(Single.just(PuzzleRating(puzzleId = id, rating = rating)))
                .toObservable()
                .asFlow()
                .catch { onError(it) }
                .onEach { savePuzzleRatingToDB(it) }

    fun getPuzzleSolution(id: Long): Flow<List<PuzzleSolution>> {
        disposable += restService.getPuzzleSolutions(id)
                .subscribe({ runBlocking { savePuzzleSolutionsToDB(it) } }, this::onError)

        return dao.getPuzzleSolution(id)
                .catch { emit(emptyList()) }
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleCollectionSolutions(): Flow<Map<Long, Int>> {
        return dao.getPuzzleCollectionSolutions()
                .catch { emit(emptyList()) }
                .onEach { Log.d("PuzzleRepository", it.toString()) }
                .map { it.sortedBy { it.collectionId } }
                .distinctUntilChanged()
                .map { it.associateBy({ it.collectionId }, { it.count }) }
    }

    fun markPuzzleSolved(id: Long, record: PuzzleSolution): Flow<PuzzleSolution> =
        restService.markPuzzleSolved(id, record)
                .doOnComplete { Log.d("PuzzleRepository", "solve $id: $record") }
                .andThen(Single.just(record))
                .also {
                    it.map(::listOf)
                        .subscribe({ runBlocking { savePuzzleSolutionsToDB(it) } }, this::onError)
                        .addToDisposable(disposable)
                }
                .toObservable()
                .asFlow()

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
