package io.zenandroid.onlinego.ui.composables

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLocaleTest {

  private val defaults = NSUserDefaults.standardUserDefaults
  private val savedLanguages = defaults.objectForKey(APPLE_LANGUAGES)

  @AfterTest
  fun tearDown() {
    if (savedLanguages == null) defaults.removeObjectForKey(APPLE_LANGUAGES)
    else defaults.setObject(savedLanguages, APPLE_LANGUAGES)
  }

  @Test
  fun theLaunchLocaleIsKeptWhileTheLanguageMatchesIt() {
    defaults.setObject(listOf(NSLocale.currentLocale.languageCode), APPLE_LANGUAGES)

    assertEquals(NSLocale.currentLocale.localeIdentifier, appLocale().localeIdentifier)
  }

  @Test
  fun aLanguagePickedSinceLaunchKeepsTheDeviceRegion() {
    val language = if (NSLocale.currentLocale.languageCode == "de") "fr" else "de"
    defaults.setObject(listOf(language), APPLE_LANGUAGES)

    val locale = appLocale()

    assertEquals(language, locale.languageCode)
    assertEquals(NSLocale.currentLocale.countryCode, locale.countryCode)
  }

  private companion object {
    const val APPLE_LANGUAGES = "AppleLanguages"
  }
}
