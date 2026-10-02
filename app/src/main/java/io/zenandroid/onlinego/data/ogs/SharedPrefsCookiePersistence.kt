package io.zenandroid.onlinego.data.ogs

import android.content.Context
import androidx.core.content.edit
import io.zenandroid.onlinego.utils.appJson

private const val SESSION_PREFS = "ogs_session"
private const val LEGACY_PREFS = "CookiePersistence"

class SharedPrefsCookiePersistence(private val context: Context) : SessionCookiePersistence {

  private val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

  override fun load(): Map<String, StoredCookie> =
    prefs.all
      .mapNotNull { (name, serialized) ->
        (serialized as? String)
          ?.let { runCatching { appJson.decodeFromString<StoredCookie>(it) }.getOrNull() }
          ?.let { name to it }
      }
      .toMap()
      .ifEmpty(::importLegacySession)

  override fun save(cookies: Map<String, StoredCookie>) {
    prefs.edit(commit = true) {
      clear()
      cookies.forEach { (name, cookie) -> putString(name, appJson.encodeToString(cookie)) }
    }
  }

  private fun importLegacySession(): Map<String, StoredCookie> {
    val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
    return importLegacyCookies(legacy.all.values.filterIsInstance<String>())
      .also { if (it.isNotEmpty()) save(it) }
  }
}
