package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.ogs.UIConfig
import kotlinx.serialization.SerializationException
import kotlin.time.Clock

private const val UICONFIG_KEY = "UICONFIG_KEY"
private const val UICONFIG_TIMESTAMP_KEY = "UICONFIG_TIMESTAMP_KEY"
private const val PUZZLE_REFRESH = "PUZZLE_DIRECTORY_REFRESH"

interface KeyValueStore {
  fun getString(key: String): String?
  fun getLong(key: String, default: Long): Long
  fun putString(key: String, value: String)
  fun putLong(key: String, value: Long)
}

/**
 * Created by alex on 07/11/2017.
 */
class PersistenceManager(private val prefs: KeyValueStore) {

  fun storeUIConfig(uiConfig: UIConfig) {
    prefs.putString(UICONFIG_KEY, appJson.encodeToString(uiConfig))
    prefs.putLong(UICONFIG_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())
  }

  /**
   * A blob written by an older build may no longer parse. Returning null makes the caller treat
   * the session as absent and re-fetch, which costs a login; throwing here would crash on launch.
   */
  fun getUIConfig(): UIConfig? =
    prefs.getString(UICONFIG_KEY)?.let {
      try {
        appJson.decodeFromString<UIConfig>(it)
      } catch (e: SerializationException) {
        CrashReporter.recordException(e)
        null
      }
    }

  fun getUIConfigTimestamp(): Long = prefs.getLong(UICONFIG_TIMESTAMP_KEY, 0)

  var puzzleCollectionLastRefresh: Long
    get() = prefs.getLong(PUZZLE_REFRESH, 0)
    set(value) = prefs.putLong(PUZZLE_REFRESH, value)
}