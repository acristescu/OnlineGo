package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.core.content.edit
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import kotlinx.serialization.SerializationException

private const val UICONFIG_KEY = "UICONFIG_KEY"
private const val UICONFIG_TIMESTAMP_KEY = "UICONFIG_TIMESTAMP_KEY"
private const val PUZZLE_REFRESH = "PUZZLE_DIRECTORY_REFRESH"

/**
 * Created by alex on 07/11/2017.
 */
class PersistenceManager(context: Context) {
  private val prefs by lazy { context.getSharedPreferences("login", Context.MODE_PRIVATE) }

  fun storeUIConfig(uiConfig: UIConfig) {
    prefs.edit {
      putString(UICONFIG_KEY, appJson.encodeToString(uiConfig))
      putLong(UICONFIG_TIMESTAMP_KEY, System.currentTimeMillis())
    }
  }

  /**
   * A blob written by an older build may no longer parse. Returning null makes the caller treat
   * the session as absent and re-fetch, which costs a login; throwing here would crash on launch.
   */
  fun getUIConfig(): UIConfig? =
    prefs.getString(UICONFIG_KEY, null)?.let {
      try {
        appJson.decodeFromString<UIConfig>(it)
      } catch (e: SerializationException) {
        recordException(e)
        null
      }
    }

  fun getUIConfigTimestamp(): Long = prefs.getLong(UICONFIG_TIMESTAMP_KEY, 0)

  var puzzleCollectionLastRefresh: Long
    get() = prefs.getLong(PUZZLE_REFRESH, 0)
    set(value) = prefs.edit { putLong(PUZZLE_REFRESH, value) }
}