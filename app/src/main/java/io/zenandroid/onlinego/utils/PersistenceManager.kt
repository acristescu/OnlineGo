package io.zenandroid.onlinego.utils

import android.content.Context
import com.squareup.moshi.Moshi
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.model.ogs.LoginToken
import io.zenandroid.onlinego.model.ogs.UIConfig
import java.util.*

/**
 * Created by alex on 07/11/2017.
 */
class PersistenceManager {
    companion object {
        val instance = PersistenceManager()

        const val TOKEN_KEY = "TOKEN_KEY"
        const val UICONFIG_KEY = "UICONFIG_KEY"
        const val TOKEN_EXPIRY_KEY = "TOKEN_EXPIRY_KEY"
    }

    private val prefs = OnlineGoApplication.instance.getSharedPreferences("login", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()

    fun storeToken(token: LoginToken, tokenExpiry: Date) {
        prefs.edit()
            .putString(TOKEN_KEY, moshi.adapter(LoginToken::class.java).toJson(token))
            .putLong(TOKEN_EXPIRY_KEY, tokenExpiry.time)
            .apply()
    }

    fun storeUIConfig(uiConfig: UIConfig) {
        prefs.edit()
            .putString(UICONFIG_KEY, moshi.adapter(UIConfig::class.java).toJson(uiConfig))
            .apply()
    }

    fun getUIConfig(): UIConfig? =
        prefs.getString(UICONFIG_KEY, null)?.let {
            return moshi.adapter(UIConfig::class.java).fromJson(it)
        }

    fun getToken(): LoginToken? =
            prefs.getString(TOKEN_KEY, null)?.let {
                return moshi.adapter(LoginToken::class.java).fromJson(it)
            }

    fun getTokenExpiry(): Date = Date(prefs.getLong(TOKEN_EXPIRY_KEY, 0))

}