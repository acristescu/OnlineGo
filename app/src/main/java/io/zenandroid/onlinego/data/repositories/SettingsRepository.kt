package io.zenandroid.onlinego.data.repositories

import androidx.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication

private const val SHOW_RANKS = "show_ranks"
private const val SHOW_COORDINATES = "show_coordinates"
private const val VIBRATE = "vibrate_on_move"

object SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var showRanks: Boolean
        get() = prefs.getBoolean(SHOW_RANKS, true)
        set(value) = prefs.edit().putBoolean(SHOW_RANKS, value).apply()

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean(VIBRATE, true)
        set(value) = prefs.edit().putBoolean(VIBRATE, value).apply()
}