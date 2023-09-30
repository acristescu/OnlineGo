package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.db.GameDao
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

class PuzzleRepository(
        private val restService: OGSRestService,
        private val dao: GameDao
) {

    private val refreshCooldownSeconds = 60 * 60 * 24

    private val disposable = CompositeDisposable()
    private val customMarkPattern = "<(.):([A-H]|[J-T]\\d{1,2})>".toPattern()
    private val headerWithMissingSpaceRegex = "#(?!\\s|#)".toRegex()

    fun getAllPuzzleCollections(): Flowable<List<PuzzleCollection>> {
        val lastRefresh = now().getEpochSecond() - PersistenceManager.puzzleCollectionLastRefresh
        disposable += dao.getPuzzleCollectionCount().map { it < 1 }
            .subscribeOn(Schedulers.computation())
            .subscribe { noCollections ->
                if(lastRefresh > refreshCooldownSeconds || noCollections) {
                    disposable += restService.getPuzzleCollections()
                            .subscribe(this::saveCollectionsToDB, this::onError)
                    PersistenceManager.puzzleCollectionLastRefresh = now().getEpochSecond()
                }
            }

        return dao.getAllPuzzleCollections()
                .doOnNext { it.forEach{c -> Log.d("PuzzleRepository", c.toString())} }
                .distinctUntilChanged()
    }

    fun getRecentPuzzleCollections(): Flowable<List<VisitedPuzzleCollection>> {
        return dao.getRecentPuzzleCollections()
                .doOnNext { it.forEach{c -> Log.d("PuzzleRepository", c.toString())} }
                .distinctUntilChanged()
    }

    fun markPuzzleCollectionVisited(id: Long): Single<VisitedPuzzleCollection> {
        val visit = VisitedPuzzleCollection(id)
        return dao.insertPuzzleCollectionVisit(visit).andThen(Single.just(visit))
                .doOnSuccess { Log.d("PuzzleRepository", it.toString()) }
    }

    fun getPuzzleCollection(id: Long): Flowable<PuzzleCollection> {
        disposable += restService.getPuzzleCollection(id).map(::listOf)
                .subscribe(this::saveCollectionsToDB, this::onError)

        return dao.getPuzzleCollection(id)
                .doOnNext { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleCollectionContents(id: Long): Flowable<List<Puzzle>> {
        disposable += restService.getPuzzleCollectionContents(id)
                .subscribe(this::savePuzzlesToDB, this::onError)

        return dao.getPuzzleCollectionPuzzles(id)
                .doOnNext { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzle(id: Long): Flowable<Puzzle> {
        disposable += restService.getPuzzle(id).map(::listOf)
                .subscribe(this::savePuzzlesToDB, this::onError)
        disposable += restService.getPuzzleRating(id)
                .subscribe(this::savePuzzleRatingToDB, this::onError)
        disposable += restService.getPuzzleSolutions(id)
                .subscribe(this::savePuzzleSolutionsToDB, this::onError)

        return dao.getPuzzle(id)
                .doOnNext { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleRating(id: Long): Flowable<PuzzleRating> {
        disposable += restService.getPuzzleRating(id)
                .map { it.copy(puzzleId = id) }
                .subscribe(this::savePuzzleRatingToDB, this::onError)

        return dao.getPuzzleRating(id)
                .onErrorReturnItem(PuzzleRating(
                    puzzleId = id,
                    rating = -1,
                ))
                .doOnNext { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun ratePuzzle(id: Long, rating: Int): Single<PuzzleRating> =
        restService.ratePuzzle(id, rating)
                .doOnComplete { Log.d("PuzzleRepository", "rate $id: $rating") }
                .andThen(Single.just(PuzzleRating(puzzleId = id, rating = rating)))
                .also {
                    it.subscribe(this::savePuzzleRatingToDB, this::onError)
                        .addToDisposable(disposable)
                }

    fun getPuzzleSolution(id: Long): Flowable<List<PuzzleSolution>> {
        disposable += restService.getPuzzleSolutions(id)
                .subscribe(this::savePuzzleSolutionsToDB, this::onError)

        return dao.getPuzzleSolution(id)
                .onErrorReturnItem(emptyList())
                .doOnNext { Log.d("PuzzleRepository", it.toString()) }
                .distinctUntilChanged()
    }

    fun getPuzzleCollectionSolutions(): Flowable<Map<Long, Int>> {
        return dao.getPuzzleCollectionSolutions()
            .onErrorReturnItem(emptyList())
            .doOnNext { Log.d("PuzzleRepository", it.toString()) }
            .map { it.sortedBy { it.collectionId } }
            .distinctUntilChanged()
            .map { it.associateBy({ it.collectionId }, { it.count }) }
    }

    fun markPuzzleSolved(id: Long, record: PuzzleSolution): Single<PuzzleSolution> =
        restService.markPuzzleSolved(id, record)
                .doOnComplete { Log.d("PuzzleRepository", "solve $id: $record") }
                .andThen(Single.just(record))
                .also {
                    it.map(::listOf).subscribe(this::savePuzzleSolutionsToDB, this::onError)
                        .addToDisposable(disposable)
                }

    private fun saveCollectionsToDB(list: List<PuzzleCollection>) {
        dao.insertPuzzleCollections(list)
    }

    private fun savePuzzlesToDB(list: List<Puzzle>) {
        dao.insertPuzzles(list)
    }

    private fun savePuzzleRatingToDB(item: PuzzleRating) {
        dao.insertPuzzleRating(item)
    }

    private fun savePuzzleSolutionsToDB(list: List<PuzzleSolution>) {
        dao.insertPuzzleSolutions(list)
    }

    private fun onError(error: Throwable) {
        Log.e("PuzzleRepository", error.message, error)
        recordException(error)
    }
}
