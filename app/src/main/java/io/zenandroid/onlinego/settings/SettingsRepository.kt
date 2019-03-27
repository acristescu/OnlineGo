package io.zenandroid.onlinego.settings

import android.support.v7.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication

private const val SHOW_COORDINATES = "show_coordinates"

object SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

}