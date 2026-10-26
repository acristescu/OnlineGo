package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class UserDefaultsAppLanguageSetting(
  private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AppLanguageSetting {

  private val _language = MutableStateFlow(currentLanguage())

  override val language: StateFlow<AppLanguage> = _language.asStateFlow()

  override fun refresh() {
    _language.value = currentLanguage()
  }

  override fun setLanguage(language: AppLanguage) {
    defaults.setObject(listOf(language.localeTag), APPLE_LANGUAGES)
    _language.value = language
  }

  private fun currentLanguage(): AppLanguage =
    defaults.stringArrayForKey(APPLE_LANGUAGES)
      ?.firstNotNullOfOrNull { AppLanguage.fromLocaleTag(it as? String) }
      ?: AppLanguage.DEFAULT

  private companion object {
    const val APPLE_LANGUAGES = "AppleLanguages"
  }
}
