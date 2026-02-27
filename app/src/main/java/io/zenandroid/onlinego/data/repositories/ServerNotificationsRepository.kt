package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.json.JSONObject

class ServerNotificationsRepository(
  private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
  private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val notificationsHash = hashMapOf<String, JSONObject>()
  private val _notifications = MutableSharedFlow<JSONObject>()

  override fun onSocketConnected() {
    scope.launch {
      try {
        socketService.connectToServerNotifications().collect { onNewNotification(it) }
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  override fun onSocketDisconnected() {
    notificationsHash.clear()
    scope.cancel()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  private suspend fun onNewNotification(notification: JSONObject) {
    if ((notification["type"] as? String) == "delete") {
      (notification["id"] as? String)?.let {
        notificationsHash.remove(it)
      }
    } else {
      (notification["id"] as? String)?.let {
        notificationsHash[it] = notification
        _notifications.emit(notification)
        acknowledgeNotification(notification)
      }
    }
  }

  fun notificationsFlow(): Flow<JSONObject> =
    _notifications.onStart { notificationsHash.values.forEach { emit(it) } }

  suspend fun acknowledgeNotification(notification: JSONObject) {
    (notification["id"] as? String)?.let {
      socketService.deleteNotification(it)
    }
  }
}