package io.zenandroid.onlinego.utils

import co.touchlab.kermit.Logger
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.cinterop.UnsafeNumber
import platform.Foundation.NSError
import platform.Foundation.NSPOSIXErrorDomain
import platform.Foundation.NSURLErrorCallIsActive
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import platform.posix.ECONNABORTED
import platform.posix.ECONNRESET
import platform.posix.ENETDOWN
import platform.posix.ENETUNREACH
import platform.posix.ENOTCONN
import platform.posix.EPIPE
import platform.posix.ETIMEDOUT

actual object CrashReporter {
  actual fun recordException(t: Throwable) {
    reportableException(t)?.let { Logger.e("Unreported exception", it, tag = "CrashReporter") }
  }

  actual fun log(message: String) {}

  actual fun setUserId(id: String) {}

  actual fun setCustomKey(key: String, value: String) {}

  actual fun setCustomKey(key: String, value: Boolean) {}

  actual fun setCustomKey(key: String, value: Long) {}

  actual fun sendUnsentReports() {}
}

@OptIn(UnsafeNumber::class)
private val connectivityURLErrors = setOf(
  NSURLErrorNotConnectedToInternet,
  NSURLErrorNetworkConnectionLost,
  NSURLErrorCannotFindHost,
  NSURLErrorCannotConnectToHost,
  NSURLErrorDNSLookupFailed,
  NSURLErrorTimedOut,
  NSURLErrorDataNotAllowed,
  NSURLErrorInternationalRoamingOff,
  NSURLErrorCallIsActive,
)

private val connectivityPOSIXErrors = setOf(
  ECONNRESET,
  ECONNABORTED,
  ENOTCONN,
  ENETDOWN,
  ENETUNREACH,
  ETIMEDOUT,
  EPIPE,
)

@OptIn(UnsafeNumber::class)
private fun NSError.isConnectivityError() = when (domain) {
  NSURLErrorDomain -> code in connectivityURLErrors
  NSPOSIXErrorDomain, "kNWErrorDomainPOSIX" -> code.toInt() in connectivityPOSIXErrors
  else -> false
}

internal actual fun Throwable.isPlatformNetworkError() = when (this) {
  is SocketTimeoutException, is ConnectTimeoutException -> true
  is DarwinHttpRequestException -> origin.isConnectivityError()
  else -> false
}
