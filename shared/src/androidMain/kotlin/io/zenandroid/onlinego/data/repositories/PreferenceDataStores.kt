package io.zenandroid.onlinego.data.repositories

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(
  name = "settings",
  produceMigrations = { context ->
    listOf(
      SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = "io.zenandroid.onlinego_preferences"
      )
    )
  }
)

val Context.reviewPromptDataStore by preferencesDataStore(
  name = "review_prompt"
)

val Context.whatsNewDataStore by preferencesDataStore(name = "whats_new")
