package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable

/**
 * The light/dark preference.
 *
 * [storedValue] is what gets persisted and must stay stable across locales and releases - it is
 * deliberately the English text this setting used to be stored as, so existing preferences keep
 * working. It is never shown to the user; the settings screen maps each entry to a string resource.
 */
@Immutable
enum class AppTheme(
  val storedValue: String,
) {
  SYSTEM_DEFAULT("System Default"),
  LIGHT("Light"),
  DARK("Dark");

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
