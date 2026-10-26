package io.zenandroid.onlinego.di

import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ai.AnalysisEngine
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.ui.screens.main.MainActivityViewModel
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingViewModel
import io.zenandroid.onlinego.ui.screens.settings.SettingsViewModel
import io.zenandroid.onlinego.ui.screens.supporter.SupporterViewModel
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import io.zenandroid.onlinego.utils.NOOPIdlingResource
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.ReviewPromptManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val appModule = module {
  single { (androidApplication() as OnlineGoApplication).applicationScope }
  single {
    val context = androidContext()
    TutorialsSource {
      context.assets.open("tutorials.json").bufferedReader().use { it.readText() }
    }
  }
  singleOf(::AppLocaleManager)
  singleOf(::NotificationUtils)
  single { GameNotificationCanceller { get<NotificationUtils>().cancelNotification(it.toInt()) } }
  single<AnalysisEngine> { KataGoAnalysisEngine(get()) }
}

private val viewModelsModule = module {
  viewModelOf(::OnboardingViewModel)
  viewModelOf(::SupporterViewModel)
  viewModelOf(::MainActivityViewModel)
  viewModelOf(::SettingsViewModel)
}

private val espressoModule = module {
  single<CountingIdlingResource> { NOOPIdlingResource() }
}

private val playStoreModule = module {
  singleOf(::PlayStoreService)
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
  viewModelsModule,
  espressoModule,
  playStoreModule,
  reviewPromptModule
)
