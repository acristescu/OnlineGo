package io.zenandroid.onlinego.utils

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

actual object CrashReporter {
  private val crashlytics get() = FirebaseCrashlytics.getInstance()

  actual fun recordException(t: Throwable) {
    reportableException(t)?.let { crashlytics.recordException(it) }
  }

  actual fun log(message: String) = crashlytics.log(message)

  actual fun setUserId(id: String) = crashlytics.setUserId(id)

  actual fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

  actual fun setCustomKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)

  actual fun setCustomKey(key: String, value: Long) = crashlytics.setCustomKey(key, value)

  actual fun sendUnsentReports() = crashlytics.sendUnsentReports()
}

class CrashlyticsBreadcrumbWriter(
  private val minSeverity: Severity = Severity.Info,
) : LogWriter() {
  override fun isLoggable(tag: String, severity: Severity) = severity >= minSeverity

  override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
    val suffix = throwable?.let { " $it" }.orEmpty()
    CrashReporter.log("${severity.name.first()}/$tag: $message$suffix")
  }
}

internal actual fun Throwable.isPlatformNetworkError() =
  this is SocketTimeoutException ||
    this is SocketException ||
    this is ConnectException ||
    this is UnknownHostException ||
      (this is ApiException && statusCode == 12501)
