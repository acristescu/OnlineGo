package io.zenandroid.onlinego.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun recordException(t: Throwable) {
  if (
    t !is CancellationException &&
    t !is SocketTimeoutException &&
    t !is ConnectException &&
    t !is UnknownHostException
  ) {
    FirebaseCrashlytics.getInstance().recordException(t)
  }
}