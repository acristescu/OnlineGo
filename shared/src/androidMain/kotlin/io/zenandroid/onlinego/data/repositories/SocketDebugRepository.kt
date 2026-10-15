package io.zenandroid.onlinego.data.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

enum class SocketEventType {
  SENT,
  RECEIVED,
  STATE,
  ERROR,
}

private val TIME_FORMAT = LocalTime.Format {
  hour()
  char(':')
  minute()
  char(':')
  second()
  char('.')
  secondFraction(3)
}

data class SocketEvent(
  val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
  val type: SocketEventType,
  val tag: String,
  val message: String,
) {
  val formattedTime: String
    get() = Instant.fromEpochMilliseconds(timestamp)
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .time
      .format(TIME_FORMAT)
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

