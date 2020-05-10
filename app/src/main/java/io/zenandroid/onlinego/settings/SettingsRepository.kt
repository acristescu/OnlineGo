package io.zenandroid.onlinego.settings

import androidx.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication

private const val HIDE_RANKS = "hide_ranks"
private const val SHOW_COORDINATES = "show_coordinates"
private const val VIBRATE = "vibrate_on_move"

object SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var hideRanks: Boolean
        get() = prefs.getBoolean(HIDE_RANKS, false)
        set(value) = prefs.edit().putBoolean(HIDE_RANKS, value).apply()

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean(VIBRATE, true)
        set(value) = prefs.edit().putBoolean(VIBRATE, value).apply()
}