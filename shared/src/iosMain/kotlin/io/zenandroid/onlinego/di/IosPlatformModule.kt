package io.zenandroid.onlinego.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.zenandroid.onlinego.ai.AnalysisEngine
import io.zenandroid.onlinego.ai.UnavailableAnalysisEngine
import io.zenandroid.onlinego.data.billing.SupporterStore
import io.zenandroid.onlinego.data.billing.UnavailableSupporterStore
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.KeyValueCookiePersistence
import io.zenandroid.onlinego.data.ogs.SessionCookiePersistence
import io.zenandroid.onlinego.ui.composables.BundleSoundLoader
import io.zenandroid.onlinego.ui.composables.SoundLoader
import io.zenandroid.onlinego.utils.AppLanguageSetting
import io.zenandroid.onlinego.utils.KeyValueStore
import io.zenandroid.onlinego.utils.UserDefaultsAppLanguageSetting
import io.zenandroid.onlinego.utils.UserDefaultsKeyValueStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal const val SESSION_DEFAULTS_SUITE = "ogs_session"
internal const val LOGIN_DEFAULTS_SUITE = "login"

val iosPlatformModule = module {
  single<HttpClientEngine> { Darwin.create() }

  single<SessionCookiePersistence> {
    KeyValueCookiePersistence(UserDefaultsKeyValueStore(SESSION_DEFAULTS_SUITE))
  }
  single<KeyValueStore> { UserDefaultsKeyValueStore(LOGIN_DEFAULTS_SUITE) }
  single(SETTINGS_DATA_STORE) { preferencesDataStore("settings", get()) }
  single(REVIEW_PROMPT_DATA_STORE) { preferencesDataStore("review_prompt", get()) }
  single(WHATS_NEW_DATA_STORE) { preferencesDataStore("whats_new", get()) }
  single<SoundLoader> { BundleSoundLoader }
  single<AnalysisEngine> { UnavailableAnalysisEngine }
  single<AppLanguageSetting> { UserDefaultsAppLanguageSetting() }
  single<SupporterStore> { UnavailableSupporterStore }
  single {
    Room.databaseBuilder<Database>(name = applicationSupportPath("database.db"))
      .setDriver(BundledSQLiteDriver())
  }
}

private fun preferencesDataStore(name: String, scope: CoroutineScope) =
  PreferenceDataStoreFactory.createWithPath(scope = scope) {
    applicationSupportPath("$name.preferences_pb").toPath()
  }

private fun applicationSupportPath(fileName: String) = "${applicationSupportDirectory()}/$fileName"

@OptIn(ExperimentalForeignApi::class)
internal fun applicationSupportDirectory(): String {
  val directory = NSFileManager.defaultManager.URLForDirectory(
    directory = NSApplicationSupportDirectory,
    inDomain = NSUserDomainMask,
    appropriateForURL = null,
    create = true,
    error = null,
  )
  return requireNotNull(directory?.path)
}
