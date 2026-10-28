package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import io.zenandroid.onlinego.utils.AppLanguageSetting

@Composable
internal fun RestartOnLanguageChange(
  languageSetting: AppLanguageSetting,
  content: @Composable () -> Unit,
) {
  key(languageSetting.language.collectAsState().value) {
    content()
  }
}
