package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.AppLanguage
import kotlinx.coroutines.flow.StateFlow

interface AppLanguageSetting {
  val language: StateFlow<AppLanguage>
  fun refresh()
  fun setLanguage(language: AppLanguage)
}
