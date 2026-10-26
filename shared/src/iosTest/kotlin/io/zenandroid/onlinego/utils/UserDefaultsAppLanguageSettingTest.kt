package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.AppLanguage
import platform.Foundation.NSUserDefaults
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserDefaultsAppLanguageSettingTest {

  private val suiteName = "test-${Random.nextLong().toULong()}"
  private val defaults = NSUserDefaults(suiteName = suiteName)

  @AfterTest
  fun tearDown() {
    NSUserDefaults.standardUserDefaults.removePersistentDomainForName(suiteName)
  }

  @Test
  fun aPickedLanguageIsReadBack() {
    UserDefaultsAppLanguageSetting(defaults).setLanguage(AppLanguage.ROMANIAN)

    assertEquals(AppLanguage.ROMANIAN, UserDefaultsAppLanguageSetting(defaults).language.value)
  }

  @Test
  fun theFirstTranslatedLanguageInTheListWins() {
    defaults.setObject(listOf("ja-JP", "de-AT", "fr"), "AppleLanguages")

    assertEquals(AppLanguage.GERMAN, UserDefaultsAppLanguageSetting(defaults).language.value)
  }

  @Test
  fun untranslatedLanguagesFallBackToTheDefault() {
    defaults.setObject(listOf("ja-JP"), "AppleLanguages")

    assertEquals(AppLanguage.DEFAULT, UserDefaultsAppLanguageSetting(defaults).language.value)
  }
}
