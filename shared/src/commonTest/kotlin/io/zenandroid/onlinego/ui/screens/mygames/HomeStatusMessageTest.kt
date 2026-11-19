package io.zenandroid.onlinego.ui.screens.mygames

import io.zenandroid.onlinego.data.ogs.ConnectionStatus
import io.zenandroid.onlinego.data.repositories.GameSyncState
import kotlin.test.assertEquals
import kotlin.test.Test

class HomeStatusMessageTest {
  @Test
  fun connectionAttemptsAreNotReportedAsOffline() {
    assertEquals(listOf(HomeStatusMessage.Connecting), homeStatusMessages(ConnectionStatus.Connecting, GameSyncState()))
    assertEquals(listOf(HomeStatusMessage.Reconnecting), homeStatusMessages(ConnectionStatus.Reconnecting, GameSyncState()))
    assertEquals(listOf(HomeStatusMessage.Offline), homeStatusMessages(ConnectionStatus.Disconnected, GameSyncState()))
  }

  @Test
  fun connectionAndGameRefreshAreReportedIndependently() {
    assertEquals(
      listOf(HomeStatusMessage.Connecting, HomeStatusMessage.RefreshingGames),
      homeStatusMessages(ConnectionStatus.Connecting, GameSyncState(pendingRefreshes = 1)),
    )
    assertEquals(
      listOf(HomeStatusMessage.RefreshingGames),
      homeStatusMessages(ConnectionStatus.Connected, GameSyncState(pendingRefreshes = 2)),
    )
  }

  @Test
  fun aFailedRefreshDoesNotReportTheConnectedSocketAsOffline() {
    assertEquals(
      listOf(HomeStatusMessage.RefreshFailed),
      homeStatusMessages(ConnectionStatus.Connected, GameSyncState(lastRefreshSucceeded = false)),
    )
  }

  @Test
  fun successfulRefreshClearsTheIndicatorAndRetryShowsProgress() {
    assertEquals(emptyList<HomeStatusMessage>(), homeStatusMessages(ConnectionStatus.Connected, GameSyncState(lastRefreshSucceeded = true)))
    assertEquals(
      listOf(HomeStatusMessage.RefreshingGames),
      homeStatusMessages(ConnectionStatus.Connected, GameSyncState(pendingRefreshes = 1, lastRefreshSucceeded = false)),
    )
  }
}
