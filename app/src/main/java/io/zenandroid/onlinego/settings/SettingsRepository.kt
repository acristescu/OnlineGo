package io.zenandroid.onlinego.settings

import androidx.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication

private const val SHOW_COORDINATES = "show_coordinates"
private const val VIBRATE = "vibrate_on_move"

object SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean(VIBRATE, true)
        set(value) = prefs.edit().putBoolean(VIBRATE, value).apply()
}