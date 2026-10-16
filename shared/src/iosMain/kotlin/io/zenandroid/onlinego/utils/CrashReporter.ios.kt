package io.zenandroid.onlinego.utils

import co.touchlab.kermit.Logger

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

internal actual fun Throwable.isPlatformNetworkError() = false
