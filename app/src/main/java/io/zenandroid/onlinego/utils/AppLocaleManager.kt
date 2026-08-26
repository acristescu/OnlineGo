package io.zenandroid.onlinego.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import io.zenandroid.onlinego.data.model.AppLanguage
import io.zenandroid.onlinego.utils.AppLocaleManager.Companion.wrap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Owns the in-app language selection.
 *
 * Nothing is stored until the user picks a language: up to that point the language is whatever the
 * phone is set to, falling back to [AppLanguage.DEFAULT] for languages we do not translate. Since
 * that is also how Android picks a `values-xx` folder, the unset case needs no work at all - the
 * platform already resolves it correctly, and [wrap] leaves such contexts alone.
 *
 * Unlike the rest of the settings this one does not live in DataStore: the locale has to be known
 * while the app is still being built, in `attachBaseContext`, before any coroutine gets a chance
 * to run, so it is kept in SharedPreferences, which is synchronous by design.
 *
 * On Android 13+ the platform stores the override - [android.app.LocaleManager] persists it,
 * applies it to every context and lets the user change it (or clear it back to the phone's
 * language) from the system settings - so that is the value read back. Below 13 the choice is
 * applied by wrapping the base context of the application and of every activity.
 */
class AppLocaleManager(private val context: Context) {

  private val _language = MutableStateFlow(currentLanguage(context))

  /** The language currently in effect, resolved from the phone when nothing was ever picked. */
  val language: StateFlow<AppLanguage> = _language.asStateFlow()

  /**
   * Re-reads the language from its source of truth. Worth calling when showing the setting, since
   * on Android 13+ the user can also change it from the system settings, behind our back.
   */
  fun refresh() {
    _language.value = currentLanguage(context)
  }

  /**
   * Persists and applies [language].
   *
   * Returns true if the caller still has to recreate the activity for the change to show up, which
   * is the case below Android 13. Above it, the platform recreates activities by itself.
   */
  fun setLanguage(language: AppLanguage): Boolean {
    prefs(context).edit { putString(KEY_LANGUAGE, language.storedValue) }
    _language.value = language

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
        LocaleList.forLanguageTags(language.localeTag)
      false
    } else {
      applyToApplicationResources(language)
      true
    }
  }

  /**
   * The application context was wrapped at process start and its base context cannot be swapped
   * afterwards, so the configuration of the resources it already handed out is updated in place.
   * Without this, strings built off the application context - notifications, mostly - would keep
   * the previous language until the process restarts.
   */
  @Suppress("DEPRECATION")
  private fun applyToApplicationResources(language: AppLanguage) {
    Locale.setDefault(language.locale)

    val resources = context.resources
    val config = Configuration(resources.configuration).apply {
      setLocale(language.locale)
      setLayoutDirection(language.locale)
    }
    resources.updateConfiguration(config, resources.displayMetrics)
  }

  companion object {
    private const val PREFS_NAME = "locale_settings"
    private const val KEY_LANGUAGE = "app_language"

    private fun prefs(context: Context) =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The language the user explicitly picked, or null if they never did. */
    private fun storedLanguage(context: Context): AppLanguage? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // The platform is the source of truth above 13, and an empty list means "not overridden".
        val locales = context.getSystemService(android.app.LocaleManager::class.java)
          ?.applicationLocales
        locales?.takeIf { !it.isEmpty }?.let { AppLanguage.fromLocaleTag(it[0].toLanguageTag()) }
      } else {
        AppLanguage.fromStoredValue(prefs(context).getString(KEY_LANGUAGE, null))
      }

    fun currentLanguage(context: Context): AppLanguage =
      storedLanguage(context) ?: languageFromDevice()

    /**
     * The first of the phone's preferred languages that we translate, or [AppLanguage.DEFAULT].
     * Walking the whole list - not just the top one - is what Android does when resolving
     * resources, so a phone set to [German, Romanian] reports Romanian here as well.
     */
    @Suppress("DEPRECATION")
    private fun languageFromDevice(): AppLanguage {
      val config = Resources.getSystem().configuration
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        return AppLanguage.fromLocale(config.locale) ?: AppLanguage.DEFAULT
      }
      val locales = config.locales
      for (i in 0 until locales.size()) {
        AppLanguage.fromLocale(locales[i])?.let { return it }
      }
      return AppLanguage.DEFAULT
    }

    /**
     * Wraps [base] so that resources resolve in the selected language. Call from
     * `attachBaseContext`.
     *
     * A no-op on Android 13+, where the platform already applies the per-app locale, and when the
     * user never picked a language, where Android's own resolution is what we want anyway.
     */
    fun wrap(base: Context): Context {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
      val locale = storedLanguage(base)?.locale ?: return base

      Locale.setDefault(locale)
      val config = Configuration(base.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
      }
      return base.createConfigurationContext(config)
    }
  }
}
