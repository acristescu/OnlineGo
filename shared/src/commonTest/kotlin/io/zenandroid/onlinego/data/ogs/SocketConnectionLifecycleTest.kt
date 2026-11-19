package io.zenandroid.onlinego.data.ogs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class SocketConnectionLifecycleTest {
  @Test
  fun reconnectsAsSoonAsPreviousSessionFinishesClosing() = runTest {
    val closing = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var attempts = 0
    val lifecycle = SocketConnectionLifecycle(backgroundScope, { _, connected ->
      attempts++
      connected()
      try {
        awaitCancellation()
      } finally {
        if (attempts == 1) withContext(NonCancellable) {
          closing.complete(Unit)
          release.await()
        }
      }
    }, { throw it }, { error("Unexpected retry") })

    lifecycle.connect()
    runCurrent()
    lifecycle.disconnect()
    runCurrent()
    closing.await()
    lifecycle.connect()
    runCurrent()
    assertEquals(1, attempts)

    release.complete(Unit)
    runCurrent()
    assertEquals(2, attempts)
    assertEquals(0L, testScheduler.currentTime)
  }

  @Test
  fun rapidPauseResumeStillReplacesTheOldSession() = runTest {
    var attempts = 0
    val lifecycle = SocketConnectionLifecycle(backgroundScope, { _, _ ->
      attempts++
      awaitCancellation()
    }, { throw it }, {})
    lifecycle.connect()
    runCurrent()
    lifecycle.disconnect()
    lifecycle.connect()
    runCurrent()
    assertEquals(2, attempts)
  }

  @Test
  fun repeatedConnectRequestsKeepOneSessionAndPreserveQueuedMessages() = runTest {
    var attempts = 0
    val messages = mutableListOf<String>()
    val lifecycle = SocketConnectionLifecycle(backgroundScope, { outgoing, _ ->
      attempts++
      messages += outgoing.receive()
      awaitCancellation()
    }, { throw it }, {})
    lifecycle.connect()
    lifecycle.send("authenticate")
    lifecycle.connect()
    runCurrent()
    assertEquals(1, attempts)
    assertEquals(listOf("authenticate"), messages)
  }

  @Test
  fun disconnectCancelsPendingRetryAndSendingDoesNotReopenTheSocket() = runTest {
    var attempts = 0
    val lifecycle = SocketConnectionLifecycle(backgroundScope, { _, _ ->
      attempts++
      error("Network unavailable")
    }, {}, {})
    lifecycle.connect()
    runCurrent()
    advanceTimeBy(750)
    runCurrent()
    assertEquals(2, attempts)
    lifecycle.disconnect()
    lifecycle.send("game/disconnect")
    runCurrent()
    advanceTimeBy(10_000)
    runCurrent()
    assertEquals(2, attempts)
  }

  @Test
  fun successfulConnectionResetsRetryBackoff() = runTest {
    var attempts = 0
    val retries = mutableListOf<Long>()
    val lifecycle = SocketConnectionLifecycle(backgroundScope, { _, connected ->
      attempts++
      if (attempts == 3) connected()
      error("Connection lost")
    }, {}, { retries += it })
    lifecycle.connect()
    runCurrent()
    advanceTimeBy(750)
    runCurrent()
    advanceTimeBy(1500)
    runCurrent()
    assertEquals(listOf(750L, 1500L, 750L), retries)
  }
}
