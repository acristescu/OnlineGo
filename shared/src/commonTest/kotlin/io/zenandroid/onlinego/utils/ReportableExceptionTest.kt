package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.ogs.OGSApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ReportableExceptionTest {

  @Test
  fun anOrdinaryExceptionIsReportedAsIs() {
    val t = IllegalStateException("boom")
    assertSame(t, reportableException(t))
  }

  @Test
  fun cancellationIsNotReported() {
    assertNull(reportableException(CancellationException("left the screen")))
  }

  @Test
  fun anExceptionCausedByCancellationIsNotReported() {
    assertNull(reportableException(Exception("wrapped", CancellationException("left the screen"))))
  }

  @Test
  fun aServerErrorIsWrappedInServerException() {
    val t = OGSApiException(503, "unavailable")
    val reported = assertIs<ServerException>(reportableException(t))
    assertSame(t, reported.cause)
  }

  @Test
  fun anExceptionCausedByAServerErrorIsWrappedInServerException() {
    val t = Exception("request failed", OGSApiException(500, null))
    val reported = assertIs<ServerException>(reportableException(t))
    assertSame(t, reported.cause)
  }

  @Test
  fun aClientErrorIsReportedAsIs() {
    val t = OGSApiException(404, "not found")
    assertSame(t, reportableException(t))
  }
}
