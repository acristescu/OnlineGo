package io.zenandroid.onlinego.utils

import android.content.Context
import com.squareup.moshi.Moshi
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.model.ogs.UIConfig

/**
 * Created by alex on 07/11/2017.
 */
class PersistenceManager {
    companion object {
        val instance = PersistenceManager()

        const val UICONFIG_KEY = "UICONFIG_KEY"
    }

    private val prefs = OnlineGoApplication.instance.getSharedPreferences("login", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()

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

}