package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.HistoricGamesMetadata
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class FinishedGamesRepository(
        private val restService: OGSRestService,
        private val userSessionRepository: UserSessionRepository,
        private val gameDao: GameDao
): SocketConnectedRepository {
    data class HistoricGamesRepositoryResult(
            val games: List<Game>,
            val loading: Boolean,
            val loadedLastPage: Boolean
    )

    private val subscriptions = CompositeDisposable()

    private var hasFetchedAllHistoricGames = false
    private var oldestGameFetchedEndedAt: Long? = null
    private var newestGameFetchedEndedAt: Long? = null

    override fun onSocketConnected() {
        gameDao.monitorHistoricGameMetadata()
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .subscribe(this::onMetadata, { onError(it, "monitorHistoricGameMetadata") })
                .addToDisposable(subscriptions)
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
    }

    fun getRecentlyFinishedGames(): Flowable<List<Game>> {
        fetchRecentlyFinishedGames()
        return gameDao
                .monitorRecentGames(userSessionRepository.userId)
    }

    private fun onError(t: Throwable, request: String) {
        var message = request
        if(t is retrofit2.HttpException) {
            message = "$request: ${t.response()?.errorBody()?.string()}"
            if(t.code() == 429) {
                FirebaseCrashlytics.getInstance().setCustomKey("HIT_RATE_LIMITER", true)
            }
        }
        recordException(Exception(message, t))
        Log.e("FinishedGameRepository", message, t)
    }

    fun getHistoricGames(endedBefore: Long?): Flowable<HistoricGamesRepositoryResult> {
        val dbObservable = if(endedBefore == null) {
            gameDao.monitorFinishedNotRecentGames(userSessionRepository.userId)
        } else {
            gameDao.monitorFinishedGamesEndedBefore(userSessionRepository.userId, endedBefore)
        }

        return dbObservable.distinctUntilChanged()
                .map {
                    if(it.size < 10 && hasFetchedAllHistoricGames) {
                        return@map HistoricGamesRepositoryResult(it, loading = false, loadedLastPage = true)
                    } else if(it.size < 10) {
                        fetchMoreHistoricGames()
                        return@map HistoricGamesRepositoryResult(it, loading = true, loadedLastPage = false)
                    } else {
                        return@map HistoricGamesRepositoryResult(it, loading = false, loadedLastPage = false)
                    }
                }
    }

    private fun fetchRecentlyFinishedGames() {
        restService.fetchHistoricGamesAfter(newestGameFetchedEndedAt)
                .map { it.map(OGSGame::id) }
                .map { it - gameDao.getHistoricGamesThatDontNeedUpdating(it).toSet() }
                .flattenAsObservable { it }
                .flatMapSingle { restService.fetchGame(it) }
                .map (Game.Companion::fromOGSGame)
                .toList()
                .retryWhen (this::retryIOException)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(
                        { onHistoricGames(it) },
                        { onError(it, "fetchRecentlyFinishedGames") }
                ).addToDisposable(subscriptions)
    }

    private var historicGamesRequestInFlight = false
    private var lastHistoricGamesRequestTimestamp = -1L

    @Synchronized
    private fun fetchMoreHistoricGames() {
        if(!historicGamesRequestInFlight) {
            historicGamesRequestInFlight = true

            val now = System.currentTimeMillis()
            val shouldThrottle = now - lastHistoricGamesRequestTimestamp < 30_000
            lastHistoricGamesRequestTimestamp = now

            restService.fetchHistoricGamesBefore(oldestGameFetchedEndedAt)
                    .doOnSuccess {
                        if (it.isEmpty()) {
                            val newMetadata = HistoricGamesMetadata(
                                    oldestGameEnded = oldestGameFetchedEndedAt,
                                    newestGameEnded = newestGameFetchedEndedAt,
                                    loadedOldestGame = true
                            )
                            gameDao.updateHistoricGameMetadata(newMetadata)
                            onMetadata(newMetadata)
                        }
                    }
                    .map { it.map(OGSGame::id) }
                    .map { it - gameDao.getHistoricGamesThatDontNeedUpdating(it) }
                    .flattenAsObservable { it }
                    .concatMap { Observable.just(it).delay(if(shouldThrottle) 1L else 0L, TimeUnit.SECONDS) }
                    .flatMapSingle { restService.fetchGame(it) }
                    .map(Game.Companion::fromOGSGame)
                    .toList()
                    .retryWhen(this::retryIOException)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single())

                    .subscribe(
                            {
                                synchronized(this@FinishedGamesRepository) {
                                    onHistoricGames(it)
                                    historicGamesRequestInFlight = false
                                }
                            },
                            {
                                synchronized(this@FinishedGamesRepository) {
                                    historicGamesRequestInFlight = false
                                }
                                onError(it, "fetchHistoricGames")
                            }
                    ).addToDisposable(subscriptions)
        } else {
            Log.i("FinishedGamesRepository", "Skipped fetchHistoricGames because request already in flight")
        }
    }

    private fun onHistoricGames(games: List<Game>) {
        val oldestGame = games.minByOrNull { it.ended ?: Long.MAX_VALUE }
        val newOldestDate = when {
            oldestGameFetchedEndedAt == null -> {
                oldestGame?.ended
            }
            oldestGame?.ended == null -> {
                oldestGameFetchedEndedAt
            }
            else -> {
                min(oldestGameFetchedEndedAt!!, oldestGame.ended)
            }
        }

        val newestGame = games.maxByOrNull { it.ended ?: Long.MIN_VALUE }
        val newNewestDate = when {
            newestGameFetchedEndedAt == null -> {
                newestGame?.ended
            }
            newestGame?.ended == null -> {
                newestGameFetchedEndedAt
            }
            else -> {
                max(newestGameFetchedEndedAt!!, newestGame.ended)
            }
        }
        val metadata = HistoricGamesMetadata(
                oldestGameEnded = newOldestDate,
                newestGameEnded = newNewestDate,
                loadedOldestGame = hasFetchedAllHistoricGames
        )
        gameDao.insertHistoricGames(games, metadata)
        onMetadata(metadata)
    }

    private fun onMetadata(metadata: HistoricGamesMetadata) {
        hasFetchedAllHistoricGames = metadata.loadedOldestGame ?: false
        oldestGameFetchedEndedAt = metadata.oldestGameEnded
        newestGameFetchedEndedAt = metadata.newestGameEnded
    }

    private fun retryIOException(it: Flowable<Throwable>) =
            it.flatMap {
                when (it) {
                    is IOException -> Flowable.timer(15, TimeUnit.SECONDS)
                    else -> Flowable.error<Long>(it)
                }
            }

}