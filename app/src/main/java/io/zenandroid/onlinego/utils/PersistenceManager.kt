package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.UIConfig

private const val UICONFIG_KEY = "UICONFIG_KEY"
private const val UICONFIG_TIMESTAMP_KEY = "UICONFIG_TIMESTAMP_KEY"
private const val PUZZLE_REFRESH = "PUZZLE_DIRECTORY_REFRESH"

/**
 * Created by alex on 07/11/2017.
 */
object PersistenceManager {
  private val prefs =
    OnlineGoApplication.instance.getSharedPreferences("login", Context.MODE_PRIVATE)
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  fun storeUIConfig(uiConfig: UIConfig) {
    prefs.edit {
      putString(UICONFIG_KEY, moshi.adapter(UIConfig::class.java).toJson(uiConfig))
      putLong(UICONFIG_TIMESTAMP_KEY, System.currentTimeMillis())
    }
  }

  fun getUIConfig(): UIConfig? =
    prefs.getString(UICONFIG_KEY, null)?.let {
      return moshi.adapter(UIConfig::class.java).fromJson(it)
    }

  fun getUIConfigTimestamp(): Long = prefs.getLong(UICONFIG_TIMESTAMP_KEY, 0)

  var puzzleCollectionLastRefresh: Long = prefs.getLong(PUZZLE_REFRESH, 0)
    set(value) {
      if (field != value) {
        prefs.edit { putLong(PUZZLE_REFRESH, value) }
      }
      field = value
    }
}