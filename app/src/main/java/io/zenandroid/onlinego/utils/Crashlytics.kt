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
  if (
    t !is CancellationException &&
    t !is SocketTimeoutException &&
    t !is ConnectException &&
    t !is UnknownHostException
  ) {
    FirebaseCrashlytics.getInstance().recordException(t)
  }
}

fun analyticsReportScreen(screenName: String) {
  FirebaseAnalytics.getInstance(OnlineGoApplication.instance).logEvent(
    FirebaseAnalytics.Event.SCREEN_VIEW,
    bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
  )
}