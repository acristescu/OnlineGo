package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ServerNotificationsRepository(
  private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
  private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val notificationsHash = hashMapOf<String, JsonObject>()
  private val _notifications = MutableSharedFlow<JsonObject>()

  override fun onSocketConnected() {
    scope.launch {
      try {
        socketService.connectToServerNotifications().collect { onNewNotification(it) }
      } catch (e: Exception) {
        CrashReporter.recordException(e)
      }
    }
  }

  override fun onSocketDisconnected() {
    notificationsHash.clear()
    scope.cancel()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  private suspend fun onNewNotification(notification: JsonObject) {
    if (notification["type"]?.jsonPrimitive?.contentOrNull == "delete") {
      notification["id"]?.jsonPrimitive?.contentOrNull?.let {
        notificationsHash.remove(it)
      }
    } else {
      notification["id"]?.jsonPrimitive?.contentOrNull?.let {
        notificationsHash[it] = notification
        _notifications.emit(notification)
        acknowledgeNotification(notification)
      }
    }
  }

  fun notificationsFlow(): Flow<JsonObject> =
    _notifications.onStart { notificationsHash.values.forEach { emit(it) } }

  suspend fun acknowledgeNotification(notification: JsonObject) {
    notification["id"]?.jsonPrimitive?.contentOrNull?.let {
      socketService.deleteNotification(it)
    }
  }
}