package io.zenandroid.onlinego.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SingleFlightTest {
  @Test
  fun overlappingRequestsShareOneRefreshButLaterRequestsFetchAgain() = runTest {
    val refresh = SingleFlight<Int>()
    val release = CompletableDeferred<Unit>()
    var calls = 0
    val first = async { refresh.run { calls++; release.await(); 42 } }
    runCurrent()
    val second = async { refresh.run { calls++; 99 } }
    runCurrent()
    assertEquals(1, calls)
    release.complete(Unit)
    assertEquals(42, first.await())
    assertEquals(42, second.await())
    assertEquals(43, refresh.run { calls++; 43 })
    assertEquals(2, calls)
  }

  @Test
  fun failureReachesAllWaitersAndAllowsRetry() = runTest {
    val refresh = SingleFlight<Int>()
    val release = CompletableDeferred<Unit>()
    val first = async { runCatching { refresh.run { release.await(); error("offline") } } }
    runCurrent()
    val second = async { runCatching { refresh.run { 42 } } }
    runCurrent()
    release.complete(Unit)
    assertEquals("offline", first.await().exceptionOrNull()?.message)
    assertEquals("offline", second.await().exceptionOrNull()?.message)
    assertEquals(42, refresh.run { 42 })
  }

  @Test
  fun cancellingAWaiterDoesNotCancelTheRefresh() = runTest {
    val refresh = SingleFlight<Int>()
    val release = CompletableDeferred<Unit>()
    val first = async { refresh.run { release.await(); 42 } }
    runCurrent()
    val second = async { refresh.run { error("Duplicate refresh") } }
    runCurrent()
    second.cancelAndJoin()
    release.complete(Unit)
    assertEquals(42, first.await())
  }

  @Test
  fun cancellingTheOwnerReleasesWaitersAndAllowsRetry() = runTest {
    val refresh = SingleFlight<Int>()
    val first = async { refresh.run { awaitCancellation() } }
    runCurrent()
    val second = async { runCatching { refresh.run { 42 } } }
    runCurrent()
    first.cancelAndJoin()
    assertTrue(second.await().isFailure)
    assertEquals(42, refresh.run { 42 })
  }
}
