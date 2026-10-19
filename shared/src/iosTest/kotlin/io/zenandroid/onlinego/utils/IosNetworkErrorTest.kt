package io.zenandroid.onlinego.utils

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.cinterop.UnsafeNumber
import kotlinx.io.IOException
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSPOSIXErrorDomain
import platform.Foundation.NSURLErrorBadServerResponse
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import platform.posix.ECONNRESET
import platform.posix.ENOTCONN
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(UnsafeNumber::class)
class IosNetworkErrorTest {

  @Test
  fun connectivityFailuresAreNotReported() {
    listOf(
      darwinError(NSURLErrorDomain, NSURLErrorNotConnectedToInternet),
      darwinError(NSURLErrorDomain, NSURLErrorNetworkConnectionLost),
      darwinError(NSURLErrorDomain, NSURLErrorCannotFindHost),
      darwinError(NSURLErrorDomain, NSURLErrorTimedOut),
      darwinError(NSPOSIXErrorDomain, ENOTCONN.toLong()),
      darwinError("kNWErrorDomainPOSIX", ECONNRESET.toLong()),
      SocketTimeoutException("timeout"),
      ConnectTimeoutException("timeout"),
    ).forEach { assertNull(reportableException(it), "$it") }
  }

  @Test
  fun aNetworkFailureAsTheCauseIsNotReported() {
    val cause = darwinError(NSURLErrorDomain, NSURLErrorNotConnectedToInternet)
    assertNull(reportableException(Exception("request failed", cause)))
  }

  @Test
  fun otherDarwinErrorsAreReported() {
    listOf(
      darwinError(NSURLErrorDomain, NSURLErrorBadServerResponse),
      darwinError(NSCocoaErrorDomain, 4),
    ).forEach { assertSame(it, reportableException(it)) }
  }

  @Test
  fun otherIOErrorsAreReported() {
    val t = IOException("disk full")
    assertSame(t, reportableException(t))
  }

  private fun darwinError(domain: String?, code: Long) =
    DarwinHttpRequestException(NSError.errorWithDomain(domain, code, null))
}
