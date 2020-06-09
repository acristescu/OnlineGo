package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.UIConfig

private const val UICONFIG_KEY = "UICONFIG_KEY"
private const val VISITED_JOSEKI = "VISITED_JOSEKI"
private const val WHATS_NEW = "WHATS_NEW"

/**
 * Created by alex on 07/11/2017.
 */
object PersistenceManager {
    private val prefs = OnlineGoApplication.instance.getSharedPreferences("login", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun storeUIConfig(uiConfig: UIConfig) {
        prefs.edit()
            .putString(UICONFIG_KEY, moshi.adapter(UIConfig::class.java).toJson(uiConfig))
            .apply()
    }

    fun deleteUIConfig() {
        prefs.edit().remove(UICONFIG_KEY).apply()
    }

    fun getUIConfig(): UIConfig? =
        prefs.getString(UICONFIG_KEY, null)?.let {
            return moshi.adapter(UIConfig::class.java).fromJson(it)
        }

    var visitedJosekiExplorer: Boolean = prefs.getBoolean(VISITED_JOSEKI, false)
        set(value) {
            if(field != value) {
                prefs.edit { putBoolean(VISITED_JOSEKI, value) }
            }
            field = value
        }

    var lastWhatsNewText: String? = prefs.getString(WHATS_NEW, null)
        set(value) {
            if(field != value) {
                prefs.edit { putString(WHATS_NEW, value) }
            }
            field = value
        }
}