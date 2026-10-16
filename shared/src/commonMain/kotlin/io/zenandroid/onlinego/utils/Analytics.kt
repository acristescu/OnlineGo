package io.zenandroid.onlinego.utils

interface Analytics {
  fun logEvent(name: String, params: Map<String, String?> = emptyMap())

  fun logScreenView(screenName: String)
}
