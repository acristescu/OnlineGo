package io.zenandroid.onlinego.data.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SocketEventType {
  SENT,
  RECEIVED,
  STATE,
  ERROR,
}

data class SocketEvent(
  val timestamp: Long = System.currentTimeMillis(),
  val type: SocketEventType,
  val tag: String,
  val message: String,
) {
  val formattedTime: String
    get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

class SocketDebugRepository {

  companion object {
    private const val MAX_EVENTS = 1000
  }

  private val _events = MutableStateFlow<List<SocketEvent>>(emptyList())
  val events: StateFlow<List<SocketEvent>> = _events.asStateFlow()

  private val _connectionState = MutableStateFlow("Disconnected")
  val connectionState: StateFlow<String> = _connectionState.asStateFlow()

  fun logSent(tag: String, message: String) {
    addEvent(SocketEvent(type = SocketEventType.SENT, tag = tag, message = message))
  }

  fun logReceived(tag: String, message: String) {
    addEvent(SocketEvent(type = SocketEventType.RECEIVED, tag = tag, message = message))
  }

  fun logState(tag: String, message: String) {
    addEvent(SocketEvent(type = SocketEventType.STATE, tag = tag, message = message))
  }

  fun logError(tag: String, message: String) {
    addEvent(SocketEvent(type = SocketEventType.ERROR, tag = tag, message = message))
  }

  fun updateConnectionState(state: String) {
    _connectionState.value = state
  }

  fun clear() {
    _events.value = emptyList()
  }

  private fun addEvent(event: SocketEvent) {
    _events.value = (_events.value + event).takeLast(MAX_EVENTS)
  }
}

