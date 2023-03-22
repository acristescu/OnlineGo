package io.zenandroid.onlinego.utils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.OnlineGoApplication
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun recordException(t: Throwable) {
  if (!t.isNetworkError() && !t.cause.isNetworkError()) {
    FirebaseCrashlytics.getInstance().recordException(t)
  }
}

private fun Throwable?.isNetworkError() =
  this is CancellationException ||
    this is SocketTimeoutException ||
    this is ConnectException ||
    this is UnknownHostException

fun analyticsReportScreen(screenName: String) {
  FirebaseAnalytics.getInstance(OnlineGoApplication.instance).logEvent(
    FirebaseAnalytics.Event.SCREEN_VIEW,
    bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
  )
}