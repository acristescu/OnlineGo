package io.zenandroid.onlinego.utils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.OnlineGoApplication
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun recordException(t: Throwable) {
  if (!t.isNetworkError() && !t.cause.isNetworkError()) {
    val reported = if(t.isHttp5XXError() || t.cause.isHttp5XXError()) {
      ServerException(t)
    } else {
      t
    }
    FirebaseCrashlytics.getInstance().recordException(reported)
  }
}

private class ServerException(cause: Throwable?) : Exception(cause)

private fun Throwable?.isNetworkError() =
  this is CancellationException ||
    this is SocketTimeoutException ||
    this is ConnectException ||
    this is UnknownHostException

private fun Throwable?.isHttp5XXError() =
  this is HttpException && this.code() / 100 == 5

fun analyticsReportScreen(screenName: String) {
  FirebaseAnalytics.getInstance(OnlineGoApplication.instance).logEvent(
    FirebaseAnalytics.Event.SCREEN_VIEW,
    bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
  )
}