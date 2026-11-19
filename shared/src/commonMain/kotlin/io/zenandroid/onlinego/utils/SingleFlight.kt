package io.zenandroid.onlinego.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class SingleFlight<T> {
  private val mutex = Mutex()
  private var inFlight: CompletableDeferred<T>? = null

  suspend fun run(block: suspend () -> T): T {
    var owner = false
    val result = mutex.withLock {
      inFlight?.takeUnless { it.isCompleted } ?: CompletableDeferred<T>().also {
        inFlight = it
        owner = true
      }
    }
    if (!owner) return result.await()
    try {
      return block().also { result.complete(it) }
    } catch (t: Throwable) {
      result.completeExceptionally(t)
      throw t
    } finally {
      withContext(NonCancellable) {
        mutex.withLock {
          if (inFlight === result) inFlight = null
        }
      }
    }
  }
}
