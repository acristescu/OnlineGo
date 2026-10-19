package io.zenandroid.onlinego.di

import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeViewModel
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceViewModel
import io.zenandroid.onlinego.ui.screens.game.GameViewModel
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerViewModel
import io.zenandroid.onlinego.ui.screens.learn.LearnViewModel
import io.zenandroid.onlinego.ui.screens.localai.AiGameViewModel
import io.zenandroid.onlinego.ui.screens.main.MainActivityViewModel
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.SelectOpponentViewModel
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingViewModel
import io.zenandroid.onlinego.ui.screens.puzzle.directory.PuzzleDirectoryViewModel
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoViewModel
import io.zenandroid.onlinego.ui.screens.settings.SettingsViewModel
import io.zenandroid.onlinego.ui.screens.socketdebug.SocketDebugViewModel
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel
import io.zenandroid.onlinego.ui.screens.supporter.SupporterViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialViewModel
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.NOOPIdlingResource
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.ReviewPromptManager
import io.zenandroid.onlinego.utils.WhatsNewUtils
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
  singleOf(::WhatsNewUtils)
  singleOf(::KataGoAnalysisEngine)
}

private val viewModelsModule = module {
  viewModelOf(::TsumegoViewModel)
  viewModelOf(::PuzzleDirectoryViewModel)
  viewModelOf(::NewAutomatchChallengeViewModel)
  viewModelOf(::NewChallengeViewModel)
  viewModelOf(::SelectOpponentViewModel)
  viewModelOf(::JosekiExplorerViewModel)
  viewModelOf(::AiGameViewModel)
  viewModelOf(::StatsViewModel)
  viewModelOf(::LearnViewModel)
  viewModelOf(::TutorialViewModel)
  viewModelOf(::OnboardingViewModel)
  viewModelOf(::SupporterViewModel)
  viewModelOf(::GameViewModel)
  viewModelOf(::MainActivityViewModel)
  viewModelOf(::MyGamesViewModel)
  viewModelOf(::FaceToFaceViewModel)
  viewModelOf(::SettingsViewModel)
  viewModelOf(::SocketDebugViewModel)
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
