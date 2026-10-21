package io.zenandroid.onlinego.data.repositories

data class GameSyncState(
  val pendingRefreshes: Int = 0,
  val lastRefreshSucceeded: Boolean? = null,
) {
  val refreshing: Boolean get() = pendingRefreshes > 0
}
