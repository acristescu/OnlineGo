package io.zenandroid.onlinego.di

import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ai.AnalysisEngine
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.billing.SupporterStore
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.ReviewPromptManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

private val appModule = module {
  single { (androidApplication() as OnlineGoApplication).applicationScope }
  single {
    val context = androidContext()
    TutorialsSource {
      context.assets.open("tutorials.json").bufferedReader().use { it.readText() }
    }
  }
  singleOf(::NotificationUtils)
  single { GameNotificationCanceller { get<NotificationUtils>().cancelNotification(it.toInt()) } }
  single<AnalysisEngine> { KataGoAnalysisEngine(get()) }
}

private val playStoreModule = module {
  singleOf(::PlayStoreService) bind SupporterStore::class
}

private val reviewPromptModule = module {
  single {
    ReviewPromptManager(
      context = get(),
      reviewPromptRepository = get(),
      analytics = get(),
      applicationScope = get()
    )
  }
}

val allKoinModules = listOf(
  sharedModule,
  androidPlatformModule(logHttpBodies = BuildConfig.DEBUG),
  appModule,
  playStoreModule,
  reviewPromptModule
)
