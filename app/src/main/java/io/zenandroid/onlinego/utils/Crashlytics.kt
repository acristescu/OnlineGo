package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.core.os.bundleOf
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.ogs.httpStatusCode
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketException
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

class CrashlyticsBreadcrumbWriter(
  private val minSeverity: Severity = Severity.Info,
) : LogWriter() {
  override fun isLoggable(tag: String, severity: Severity) = severity >= minSeverity

  override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
    val suffix = throwable?.let { " $it" }.orEmpty()
    FirebaseCrashlytics.getInstance().log("${severity.name.first()}/$tag: $message$suffix")
  }
}

private class ServerException(cause: Throwable?) : Exception(cause)

private fun Throwable?.isNetworkError() =
  this is CancellationException ||
    this is SocketTimeoutException ||
    this is SocketException ||
    this is ConnectException ||
    this is UnknownHostException ||
    (this is ApiException && this.statusCode == 12501) // Google Auth cancelled

private fun Throwable?.isHttp5XXError() =
  httpStatusCode?.let { it / 100 == 5 } == true

fun analyticsReportScreen(context: Context, screenName: String) {
  FirebaseAnalytics.getInstance(context).logEvent(
    FirebaseAnalytics.Event.SCREEN_VIEW,
    bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
  )
}