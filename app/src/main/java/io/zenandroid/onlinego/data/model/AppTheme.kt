package io.zenandroid.onlinego.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.R

/**
 * The light/dark preference.
 *
 * [storedValue] is what gets persisted and must stay stable across locales and releases - it is
 * deliberately the English text this setting used to be stored as, so existing preferences keep
 * working. Only [displayNameResId] is ever shown to the user.
 */
@Immutable
enum class AppTheme(
  val storedValue: String,
  @StringRes val displayNameResId: Int,
) {
  SYSTEM_DEFAULT("System Default", R.string.settings_theme_system_default),
  LIGHT("Light", R.string.settings_theme_light),
  DARK("Dark", R.string.settings_theme_dark);

  companion object {
    val DEFAULT = SYSTEM_DEFAULT

    /**
     * Older builds persisted the label verbatim and were inconsistent about capitalisation
     * ("System default" vs "System Default"), so match case insensitively.
     */
    fun fromStoredValue(value: String?): AppTheme =
      entries.find { it.storedValue.equals(value, ignoreCase = true) } ?: DEFAULT
  }
}
