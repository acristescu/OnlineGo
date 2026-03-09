package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.HistoricGamesMetadata
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class FinishedGamesRepository(
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
  private val gameDao: GameDao
) : SocketConnectedRepository {
  data class HistoricGamesRepositoryResult(
    val games: List<Game>,
    val loading: Boolean,
    val loadedLastPage: Boolean
  )

  private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var hasFetchedAllHistoricGames = false
  private var oldestGameFetchedEndedAt: Long? = null
  private var newestGameFetchedEndedAt: Long? = null

  override fun onSocketConnected() {
    scope.launch {
      try {
        gameDao.monitorHistoricGameMetadata()
          .filterNotNull()
          .distinctUntilChanged()
          .collect { onMetadata(it) }
      } catch (e: Exception) {
        onError(e, "monitorHistoricGameMetadata")
      }
    }
  }

  override fun onSocketDisconnected() {
    scope.cancel()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  fun getRecentlyFinishedGames(): Flow<List<Game>> {
    fetchRecentlyFinishedGames()
    return userSessionRepository.userId
      .filterNotNull()
      .flatMapLatest {
        gameDao.monitorRecentGames(it).distinctUntilChanged()
      }
  }

  private fun onError(t: Throwable, request: String) {
    var message = request
    if (t is retrofit2.HttpException) {
      message = "$request: ${t.response()?.errorBody()?.string()}"
      if (t.code() == 429) {
        FirebaseCrashlytics.getInstance().setCustomKey("HIT_RATE_LIMITER", true)
      }
    }
    recordException(Exception(message, t))
    Log.e("FinishedGameRepository", message, t)
  }

  fun getHistoricGames(endedBefore: Long?): Flow<HistoricGamesRepositoryResult> {
    val dbFlow = userSessionRepository.userId
      .filterNotNull()
      .flatMapLatest {
        if (endedBefore == null) {
          gameDao.monitorFinishedNotRecentGames(it)
        } else {
          gameDao.monitorFinishedGamesEndedBefore(it, endedBefore)
        }
      }

    return dbFlow.distinctUntilChanged()
      .map {
        if (it.size < 10 && hasFetchedAllHistoricGames) {
          return@map HistoricGamesRepositoryResult(it, loading = false, loadedLastPage = true)
        } else if (it.size < 10) {
          fetchMoreHistoricGames()
          return@map HistoricGamesRepositoryResult(it, loading = true, loadedLastPage = false)
        } else {
          return@map HistoricGamesRepositoryResult(it, loading = false, loadedLastPage = false)
        }
      }
  }

  private fun fetchRecentlyFinishedGames() {
    scope.launch {
      try {
        val threeDaysAgoMicros = (System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)) * 1000
        val ogsGames =
          retryOnIOException { restService.fetchHistoricGamesAfter(threeDaysAgoMicros) }
        val games = processAndFetchGameDetails(ogsGames)
        onHistoricGames(games)
      } catch (e: Exception) {
        onError(e, "fetchRecentlyFinishedGames")
      }
    }
  }

  private suspend fun processAndFetchGameDetails(ogsGames: List<OGSGame>): List<Game> {
    val ids = ogsGames.map(OGSGame::id)
    val idsAlreadyPresent = gameDao.getHistoricGamesThatDontNeedUpdating(ids)
    return ogsGames.filter { it.id !in idsAlreadyPresent }
      .map {
        val gameData = retryOnIOException { restService.fetchTerminationGameData(it.id) }
        Game.fromOGSGame(
          it.copy(
            json = gameData
          )
        )
      }
  }

  private var historicGamesRequestInFlight = false
  private var lastHistoricGamesRequestTimestamp = -1L

  @Synchronized
  private fun fetchMoreHistoricGames() {
    if (!historicGamesRequestInFlight) {
      historicGamesRequestInFlight = true

      val now = System.currentTimeMillis()
      lastHistoricGamesRequestTimestamp = now

      scope.launch {
        try {
          val ogsGames =
            retryOnIOException { restService.fetchHistoricGamesBefore(oldestGameFetchedEndedAt) }
          if (ogsGames.isEmpty()) {
            val newMetadata = HistoricGamesMetadata(
              oldestGameEnded = oldestGameFetchedEndedAt,
              newestGameEnded = newestGameFetchedEndedAt,
              loadedOldestGame = true
            )
            gameDao.updateHistoricGameMetadata(newMetadata)
            onMetadata(newMetadata)
          }
          val games = processAndFetchGameDetails(ogsGames)
          synchronized(this@FinishedGamesRepository) {
            onHistoricGames(games)
            historicGamesRequestInFlight = false
          }
        } catch (e: Exception) {
          synchronized(this@FinishedGamesRepository) {
            historicGamesRequestInFlight = false
          }
          onError(e, "fetchHistoricGames")
        }
      }
    } else {
      Log.i(
        "FinishedGamesRepository",
        "Skipped fetchHistoricGames because request already in flight"
      )
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

  private suspend fun <T> retryOnIOException(block: suspend () -> T): T {
    while (true) {
      try {
        return block()
      } catch (e: IOException) {
        delay(10_000)
      }
    }
  }

}