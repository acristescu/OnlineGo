package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ActiveGamesSyncStateTest {
  private val rest: OGSRestService = mock()
  private val session: UserSessionRepository = mock()
  private val dao: GameDao = mock()
  private val userIds = MutableSharedFlow<Long?>(replay = 1)

  private fun repository(): ActiveGamesRepository {
    whenever(session.userId).thenReturn(userIds)
    return ActiveGamesRepository(rest, mock<OGSWebSocketService>(), session, dao)
  }

  @Test
  fun pendingRefreshAndCancellationAreVisibleWithoutReportingFailure() = runTest {
    val repository = repository()
    assertEquals(GameSyncState(), repository.syncState.value)
    val refresh = async { repository.refreshActiveGames() }
    runCurrent()
    assertEquals(GameSyncState(pendingRefreshes = 1), repository.syncState.value)
    refresh.cancelAndJoin()
    assertEquals(GameSyncState(), repository.syncState.value)
  }

  @Test
  fun successfulRefreshClearsProgress() = runTest {
    userIds.tryEmit(1L)
    whenever(rest.fetchActiveGames()).thenReturn(emptyList())
    whenever(dao.getActiveGameIds(1L)).thenReturn(emptyList())
    val repository = repository()
    repository.refreshActiveGames()
    assertEquals(GameSyncState(lastRefreshSucceeded = true), repository.syncState.value)
  }

  @Test
  fun failedRefreshClearsProgressAndPreservesFailureForTheScreen() = runTest {
    userIds.tryEmit(1L)
    whenever(rest.fetchActiveGames()).thenThrow(IllegalStateException("Server unavailable"))
    val repository = repository()
    val result = runCatching { repository.refreshActiveGames() }
    assertEquals(true, result.isFailure)
    assertEquals(GameSyncState(lastRefreshSucceeded = false), repository.syncState.value)
  }
}
