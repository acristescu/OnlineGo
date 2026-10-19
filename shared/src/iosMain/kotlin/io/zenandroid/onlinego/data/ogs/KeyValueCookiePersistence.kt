package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.utils.KeyValueStore
import io.zenandroid.onlinego.utils.appJson

private const val COOKIES_KEY = "cookies"

class KeyValueCookiePersistence(private val store: KeyValueStore) : SessionCookiePersistence {

  override fun load(): Map<String, StoredCookie> =
    store.getString(COOKIES_KEY)
      ?.let { runCatching { appJson.decodeFromString<Map<String, StoredCookie>>(it) }.getOrNull() }
      .orEmpty()

  override fun save(cookies: Map<String, StoredCookie>) =
    store.putString(COOKIES_KEY, appJson.encodeToString(cookies))
}
