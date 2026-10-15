package io.zenandroid.onlinego.utils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

class Analytics(private val firebase: FirebaseAnalytics) {

  fun logEvent(name: String, params: Map<String, String?> = emptyMap()) {
    firebase.logEvent(
      name,
      params.takeIf { it.isNotEmpty() }?.let { bundleOf(*it.toList().toTypedArray()) })
  }

  fun logScreenView(screenName: String) {
    logEvent(
      FirebaseAnalytics.Event.SCREEN_VIEW,
      mapOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
    )
  }
}
