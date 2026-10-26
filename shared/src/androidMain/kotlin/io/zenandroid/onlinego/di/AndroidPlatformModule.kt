package io.zenandroid.onlinego.di

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.HTTPConnectionFactory
import io.zenandroid.onlinego.data.ogs.SessionCookiePersistence
import io.zenandroid.onlinego.data.ogs.SharedPrefsCookiePersistence
import io.zenandroid.onlinego.data.repositories.AppDataEraser
import io.zenandroid.onlinego.data.repositories.reviewPromptDataStore
import io.zenandroid.onlinego.data.repositories.settingsDataStore
import io.zenandroid.onlinego.data.repositories.whatsNewDataStore
import io.zenandroid.onlinego.ui.composables.AssetSoundLoader
import io.zenandroid.onlinego.ui.composables.SoundLoader
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.AppLanguageSetting
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.FirebaseAppAnalytics
import io.zenandroid.onlinego.utils.KeyValueStore
import io.zenandroid.onlinego.utils.SharedPreferencesKeyValueStore
import okhttp3.OkHttpClient
import org.koin.dsl.module

fun androidPlatformModule(logHttpBodies: Boolean) = module {
  single { HTTPConnectionFactory(logBodies = logHttpBodies) }
  single { get<HTTPConnectionFactory>().buildConnection() }
  single<HttpClientEngine> { OkHttp.create { preconfigured = get<OkHttpClient>() } }

  single<SessionCookiePersistence> { SharedPrefsCookiePersistence(get()) }
  single<KeyValueStore> { SharedPreferencesKeyValueStore(get(), "login") }
  single(SETTINGS_DATA_STORE) { get<Context>().settingsDataStore }
  single(REVIEW_PROMPT_DATA_STORE) { get<Context>().reviewPromptDataStore }
  single(WHATS_NEW_DATA_STORE) { get<Context>().whatsNewDataStore }
  single<SoundLoader> { AssetSoundLoader(get<Context>().assets) }
  single { Room.databaseBuilder(get(), Database::class.java, "database.db") }

  single<Analytics> { FirebaseAppAnalytics(FirebaseAnalytics.getInstance(get())) }
  single<AppLanguageSetting> { AppLocaleManager(get()) }
  single {
    val context = get<Context>()
    AppDataEraser { context.getSystemService<ActivityManager>()?.clearApplicationUserData() }
  }
}
