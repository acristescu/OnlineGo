package io.zenandroid.onlinego.di

import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.Url
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.Glicko2HistoryConverterFactory
import io.zenandroid.onlinego.data.ogs.HTTPConnectionFactory
import io.zenandroid.onlinego.data.ogs.OGSCookieStore
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.SharedPrefsCookiePersistence
import io.zenandroid.onlinego.data.ogs.configureOGSClient
import io.zenandroid.onlinego.data.ogs.createOGSRestAPI
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AutomatchRepository
import io.zenandroid.onlinego.data.repositories.BotsRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import io.zenandroid.onlinego.data.repositories.FinishedGamesRepository
import io.zenandroid.onlinego.data.repositories.JosekiRepository
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import io.zenandroid.onlinego.data.repositories.PuzzleRepository
import io.zenandroid.onlinego.data.repositories.ReviewPromptRepository
import io.zenandroid.onlinego.data.repositories.ServerNotificationsRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.SocketDebugRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
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
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.NOOPIdlingResource
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.ReviewPromptManager
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.appJson
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val repositoriesModule = module {
  single {
    listOf(
      get<ActiveGamesRepository>(),
      get<AutomatchRepository>(),
      get<BotsRepository>(),
      get<ChallengesRepository>(),
      get<FinishedGamesRepository>(),
      get<ChatRepository>(),
      get<ServerNotificationsRepository>(),
      get<ClockDriftRepository>(),
      get<TutorialsRepository>()
    )
  }

  singleOf(::ActiveGamesRepository)
  singleOf(::AutomatchRepository)
  singleOf(::BotsRepository)
  singleOf(::ChallengesRepository)
  singleOf(::ChatRepository)
  singleOf(::FinishedGamesRepository)
  singleOf(::JosekiRepository)
  singleOf(::PuzzleRepository)
  singleOf(::PlayersRepository)
  singleOf(::ServerNotificationsRepository)
  singleOf(::SettingsRepository)
  singleOf(::AppLocaleManager)
  singleOf(::UserSessionRepository)
  singleOf(::ClockDriftRepository)
  singleOf(::TutorialsRepository)
  singleOf(::ReviewPromptRepository)
  singleOf(::SocketDebugRepository)
  singleOf(::PersistenceManager)
  singleOf(::NotificationUtils)
  singleOf(::WhatsNewUtils)
  singleOf(::KataGoAnalysisEngine)
  single { Analytics(FirebaseAnalytics.getInstance(get())) }
}

private val serverConnectionModule = module {

  single { (androidApplication() as OnlineGoApplication).applicationScope }

  singleOf(::HTTPConnectionFactory)
  single { get<HTTPConnectionFactory>().buildConnection() }

  single {
    OGSCookieStore(
      persistence = SharedPrefsCookiePersistence(get()),
      host = Url(BuildConfig.BASE_URL).host,
    )
  }

  single {
    HttpClient(OkHttp) {
      configureOGSClient(
        jsonFormat = get(),
        cookiesStorage = get<OGSCookieStore>(),
        baseUrl = BuildConfig.BASE_URL,
      )
      engine {
        preconfigured = get<OkHttpClient>()
      }
      install(WebSockets)
    }
  }

  single {
    Ktorfit.Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .httpClient(get<HttpClient>())
      .converterFactories(Glicko2HistoryConverterFactory)
      .build()
      .createOGSRestAPI()
  }

  single { appJson }

  singleOf(::OGSRestService)
  singleOf(::OGSWebSocketService)
}

private val databaseModule = module {
  single {
    Room.databaseBuilder(get(), Database::class.java, "database.db")
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()
  }

  single {
    get<Database>().gameDao()
  }

  single {
    get<Database>().puzzleDao()
  }
}

private val useCasesModule = module {
  singleOf(::GetUserStatsUseCase)
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
  repositoriesModule,
  serverConnectionModule,
  databaseModule,
  viewModelsModule,
  useCasesModule,
  espressoModule,
  playStoreModule,
  reviewPromptModule
)
