package io.zenandroid.onlinego.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.AppLanguage.Companion.DEFAULT
import java.util.Locale

/**
 * A language the app ships translations for.
 *
 * There is deliberately no "system default" entry: until the user picks something, nothing is
 * stored and the language is resolved from the phone - a phone set to Romanian starts the app in
 * Romanian - falling back to [DEFAULT] for languages we do not translate. That resolution is the
 * same rule Android uses to pick a `values-xx` folder, so the two never disagree.
 *
 * [storedValue] is what gets persisted and must stay stable across releases. [displayNameResId]
 * points at the language's own name (its endonym), so "Română" reads the same no matter which
 * language the app is currently displaying.
 *
 * To add a language later: add a `values-xx/strings.xml`, add an entry here, and add the tag to
 * `res/xml/locales_config.xml` so it also shows up in the system per-app language settings.
 */
@Immutable
enum class AppLanguage(
  val storedValue: String,
  val localeTag: String,
  val flagEmoji: String,
  @StringRes val displayNameResId: Int,
) {
  ENGLISH("en", "en", "🇬🇧", R.string.settings_language_english),
  ROMANIAN("ro", "ro", "🇷🇴", R.string.settings_language_romanian);

  val locale: Locale = Locale.forLanguageTag(localeTag)

  companion object {
    /** Used for any language we do not have translations for. */
    val DEFAULT = ENGLISH

    /** The stored choice, or null if the user never made one. */
    fun fromStoredValue(value: String?): AppLanguage? =
      entries.find { it.storedValue == value }

    /**
     * Matches on the language subtag only, so regional variants ("en-GB", "ro-MD") still resolve
     * to the right entry. Null when the language is not one we translate.
     */
    fun fromLocale(locale: Locale?): AppLanguage? {
      val language = locale?.language?.takeIf { it.isNotEmpty() } ?: return null
      return entries.find { it.locale.language == language }
    }

    fun fromLocaleTag(tag: String?): AppLanguage? =
      fromLocale(tag?.let(Locale::forLanguageTag))
  }
}
