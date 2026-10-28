package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.zenandroid.onlinego.data.model.AppLanguage
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.mygames_outcome_won
import io.zenandroid.onlinego.utils.UserDefaultsAppLanguageSetting
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RestartOnLanguageChangeTest {

  private val defaults = NSUserDefaults.standardUserDefaults
  private val savedLanguages = defaults.objectForKey(APPLE_LANGUAGES)

  @AfterTest
  fun tearDown() {
    if (savedLanguages == null) defaults.removeObjectForKey(APPLE_LANGUAGES)
    else defaults.setObject(savedLanguages, APPLE_LANGUAGES)
  }

  @Test
  fun stringsFollowALanguagePickedWhileRunning() = runComposeUiTest {
    val setting = UserDefaultsAppLanguageSetting(defaults)
    setting.setLanguage(AppLanguage.ENGLISH)
    setContent {
      RestartOnLanguageChange(setting) { Outcome() }
    }
    onNodeWithText("Won").assertExists()

    setting.setLanguage(AppLanguage.GERMAN)

    waitUntil { onAllNodesWithText("Gewonnen").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText("Won").assertDoesNotExist()
  }

  @Composable
  private fun Outcome() {
    Text(stringResource(Res.string.mygames_outcome_won))
  }

  private companion object {
    const val APPLE_LANGUAGES = "AppleLanguages"
  }
}
