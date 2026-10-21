package io.zenandroid.onlinego.data.ogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SocketConnectionLifecycle(
  scope: CoroutineScope,
  private val runConnection: suspend (Channel<String>, () -> Unit) -> Unit,
  private val onFailure: (Exception) -> Unit,
  private val onRetry: (Long) -> Unit,
) {
  private class Session(val outgoing: Channel<String> = Channel(Channel.UNLIMITED))

  private val requestedSession = MutableStateFlow<Session?>(null)

  init {
    scope.launch {
      requestedSession.collectLatest { session ->
        if (session == null) return@collectLatest
        var retryDelay = 750L
        try {
          while (true) {
            try {
              runConnection(session.outgoing) { retryDelay = 750L }
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              onFailure(e)
            }
            while (session.outgoing.tryReceive().isSuccess) Unit
            onRetry(retryDelay)
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(10_000L)
          }
        } finally {
          session.outgoing.close()
        }
      }
    }
  }

  fun connect() {
    requestedSession.update { it ?: Session() }
  }

  fun disconnect() {
    requestedSession.value = null
  }

  fun send(message: String) {
    requestedSession.value?.outgoing?.trySend(message)
  }
}
