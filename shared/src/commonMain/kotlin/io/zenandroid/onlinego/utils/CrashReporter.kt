package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.ogs.httpStatusCode
import kotlin.coroutines.cancellation.CancellationException

expect object CrashReporter {
  fun recordException(t: Throwable)

  fun log(message: String)

  fun setUserId(id: String)

  fun setCustomKey(key: String, value: String)

  fun setCustomKey(key: String, value: Boolean)

  fun setCustomKey(key: String, value: Long)

  fun sendUnsentReports()
}

internal class ServerException(cause: Throwable?) : Exception(cause)

internal expect fun Throwable.isPlatformNetworkError(): Boolean

internal fun reportableException(t: Throwable): Throwable? = when {
  t.isNetworkError() || t.cause.isNetworkError() -> null
  t.isHttp5XXError() || t.cause.isHttp5XXError() -> ServerException(t)
  else -> t
}

private fun Throwable?.isNetworkError() =
  this is CancellationException || this?.isPlatformNetworkError() == true

private fun Throwable?.isHttp5XXError() =
  httpStatusCode?.let { it / 100 == 5 } == true
