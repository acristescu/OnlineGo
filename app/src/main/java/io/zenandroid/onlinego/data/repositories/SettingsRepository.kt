package io.zenandroid.onlinego.data.repositories

import androidx.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication

private const val APP_THEME = "app_theme"
private const val SHOW_RANKS = "show_ranks"
private const val SHOW_COORDINATES = "show_coordinates"
private const val VIBRATE = "vibrate_on_move"
private const val SOUND = "sound"

class SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var appTheme: String?
        get() = prefs.getString(APP_THEME, "System default")
        set(value) = prefs.edit().putString(APP_THEME, value).apply()

    var showRanks: Boolean
        get() = prefs.getBoolean(SHOW_RANKS, true)
        set(value) = prefs.edit().putBoolean(SHOW_RANKS, value).apply()

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(value) = prefs.edit().putBoolean(VIBRATE, value).apply()

    var sound: Boolean
        get() = prefs.getBoolean(SOUND, true)
        set(value) = prefs.edit().putBoolean(SOUND, value).apply()
}