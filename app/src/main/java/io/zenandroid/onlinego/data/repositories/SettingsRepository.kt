package io.zenandroid.onlinego.data.repositories

import androidx.preference.PreferenceManager
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.BoardTheme

private const val APP_THEME = "app_theme"
private const val BOARD_THEME = "board_theme"
private const val SHOW_RANKS = "show_ranks"
private const val SHOW_COORDINATES = "show_coordinates"
private const val SOUND = "sound"
private const val MAX_VISITS = "max_ai_visits"
private const val DETAILED_ANALYSIS = "detailed_analysis"

class SettingsRepository {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    var appTheme: String?
        get() = prefs.getString(APP_THEME, "System default")
        set(value) = prefs.edit().putString(APP_THEME, value).apply()

    var boardTheme: BoardTheme
        get() = getBoardThemeFromPref()
        set(value) = prefs.edit().putString(BOARD_THEME, value.name).apply()

    private fun getBoardThemeFromPref(): BoardTheme {
        val themeFromPref: String? = prefs.getString(BOARD_THEME, BoardTheme.WOOD.name)
        if (themeFromPref != null) {
            return try {
                BoardTheme.valueOf(themeFromPref)
            } catch (e: IllegalArgumentException) {
                BoardTheme.WOOD
            }
        }
        return BoardTheme.WOOD
    }

    var showRanks: Boolean
        get() = prefs.getBoolean(SHOW_RANKS, true)
        set(value) = prefs.edit().putBoolean(SHOW_RANKS, value).apply()

    var showCoordinates: Boolean
        get() = prefs.getBoolean(SHOW_COORDINATES, false)
        set(value) = prefs.edit().putBoolean(SHOW_COORDINATES, value).apply()

    var sound: Boolean
        get() = prefs.getBoolean(SOUND, true)
        set(value) = prefs.edit().putBoolean(SOUND, value).apply()

    var maxVisits: Int
        get() = prefs.getInt(MAX_VISITS, 30)
        set(value) = prefs.edit().putInt(MAX_VISITS, value).apply()

    var detailedAnalysis: Boolean
        get() = prefs.getBoolean(DETAILED_ANALYSIS, false)
        set(value) = prefs.edit().putBoolean(DETAILED_ANALYSIS, value).apply()
}
