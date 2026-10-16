package io.zenandroid.onlinego.utils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAppAnalytics(private val firebase: FirebaseAnalytics) : Analytics {

  override fun logEvent(name: String, params: Map<String, String?>) {
    firebase.logEvent(
      name,
      params.takeIf { it.isNotEmpty() }?.let { bundleOf(*it.toList().toTypedArray()) })
  }

  override fun logScreenView(screenName: String) {
    logEvent(
      FirebaseAnalytics.Event.SCREEN_VIEW,
      mapOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
    )
  }
}
