package io.zenandroid.onlinego.ui.screens.mygames

import io.zenandroid.onlinego.data.ogs.ConnectionStatus
import io.zenandroid.onlinego.data.repositories.GameSyncState

internal enum class HomeStatusMessage {
  Connecting,
  Reconnecting,
  Offline,
  RefreshingGames,
  RefreshFailed,
}

internal fun homeStatusMessages(
  connection: ConnectionStatus,
  sync: GameSyncState,
): List<HomeStatusMessage> = buildList {
  when (connection) {
    ConnectionStatus.Connecting -> add(HomeStatusMessage.Connecting)
    ConnectionStatus.Reconnecting -> add(HomeStatusMessage.Reconnecting)
    ConnectionStatus.Disconnected -> add(HomeStatusMessage.Offline)
    ConnectionStatus.Connected -> Unit
  }
  when {
    sync.refreshing -> add(HomeStatusMessage.RefreshingGames)
    sync.lastRefreshSucceeded == false -> add(HomeStatusMessage.RefreshFailed)
  }
}
