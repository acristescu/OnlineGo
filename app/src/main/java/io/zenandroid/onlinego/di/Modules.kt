package io.zenandroid.onlinego.di

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.HTTPConnectionFactory
import io.zenandroid.onlinego.data.ogs.OGSBooleanJsonAdapter
import io.zenandroid.onlinego.data.ogs.OGSInstantJsonAdapter
import io.zenandroid.onlinego.data.ogs.OGSRestAPI
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
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
import io.zenandroid.onlinego.data.repositories.ServerNotificationsRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.mvi.Reducer
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeViewModel
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceViewModel
import io.zenandroid.onlinego.ui.screens.game.GameViewModel
import io.zenandroid.onlinego.ui.screens.joseki.HotTrackMiddleware
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerReducer
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerState
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerViewModel
import io.zenandroid.onlinego.ui.screens.joseki.LoadPositionMiddleware
import io.zenandroid.onlinego.ui.screens.joseki.TriggerLoadingMiddleware
import io.zenandroid.onlinego.ui.screens.learn.LearnViewModel
import io.zenandroid.onlinego.ui.screens.localai.AiGameReducer
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.zenandroid.onlinego.ui.screens.localai.AiGameViewModel
import io.zenandroid.onlinego.ui.screens.localai.middlewares.AIMoveMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.AnalyticsMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.EngineLifecycleMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.GameTurnMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.HintMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.OwnershipMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.StatePersistenceMiddleware
import io.zenandroid.onlinego.ui.screens.localai.middlewares.UserMoveMiddleware
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.SelectOpponentViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchMiddleware
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerReducer
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerState
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerViewModel
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingViewModel
import io.zenandroid.onlinego.ui.screens.puzzle.*
import io.zenandroid.onlinego.ui.screens.settings.SettingsViewModel
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialViewModel
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.CustomConverterFactory
import io.zenandroid.onlinego.utils.NOOPIdlingResource
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

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

  single { ActiveGamesRepository(get(), get(), get(), get()) }
  single { AutomatchRepository(get()) }
  single { BotsRepository(get()) }
  single { ChallengesRepository(get(), get(), get()) }
  single { ChatRepository(get(), get()) }
  single { FinishedGamesRepository(get(), get(), get()) }
  single { JosekiRepository(get(), get()) }
  single { PuzzleRepository(get(), get()) }
  single { PlayersRepository(get(), get(), get()) }
  single { ServerNotificationsRepository(get()) }
  single { SettingsRepository() }
  single { UserSessionRepository() }
  single { ClockDriftRepository(get()) }
  single { TutorialsRepository() }
}

private val serverConnectionModule = module {

  single { HTTPConnectionFactory(get()) }
  single { get<HTTPConnectionFactory>().buildConnection() }

  single {
    Retrofit.Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .client(get())
      .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
      .addConverterFactory(CustomConverterFactory())
      .addConverterFactory(MoshiConverterFactory.create(get()))
      .build()
      .create(OGSRestAPI::class.java)
  }

  single {
    Moshi.Builder()
      .add(java.lang.Boolean::class.java, OGSBooleanJsonAdapter())
      .add(Instant::class.java, OGSInstantJsonAdapter().nullSafe())
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  single { OGSRestService(get(), get(), get(), get()) }
  single { OGSWebSocketService(get(), get(), get(), get()) }
}

private val databaseModule = module {
  single {
    Room.databaseBuilder(get(), Database::class.java, "database.db")
      .fallbackToDestructiveMigration()
      .build()
  }

  single {
    get<Database>().gameDao()
  }
}

private val useCasesModule = module {
  single {
    GetUserStatsUseCase(get())
  }
}

private val viewModelsModule = module {
  viewModel {
    SearchPlayerViewModel(
      Store(
        SearchPlayerReducer(),
        listOf(SearchMiddleware(get())),
        SearchPlayerState()
      )
    )
  }

  viewModel {
    JosekiExplorerViewModel(
      Store(
        JosekiExplorerReducer(),
        listOf(
          LoadPositionMiddleware(get()),
          HotTrackMiddleware(),
          TriggerLoadingMiddleware(),
          io.zenandroid.onlinego.ui.screens.joseki.AnalyticsMiddleware()
        ),
        JosekiExplorerState()
      )
    )
  }

  viewModel {
    PuzzleDirectoryViewModel(get(), get())
  }

  viewModel { params ->
    PuzzleSetViewModel(get(), get(), params.get())
  }

  viewModel { params ->
    TsumegoViewModel(get(), params.get())
  }

  viewModel {
    NewAutomatchChallengeViewModel()
  }

  viewModel {
    NewChallengeViewModel()
  }

  viewModel {
    SelectOpponentViewModel(get(), get())
  }

  viewModel {
    AiGameViewModel(
      Store(
        AiGameReducer(),
        listOf(
          EngineLifecycleMiddleware(),
          AIMoveMiddleware(),
          GameTurnMiddleware(),
          UserMoveMiddleware(),
          StatePersistenceMiddleware(),
          HintMiddleware(),
          OwnershipMiddleware(),
          AnalyticsMiddleware()
        ),
        AiGameState()
      )
    )
  }

  viewModelOf(::StatsViewModel)
  viewModelOf(::LearnViewModel)
  viewModelOf(::TutorialViewModel)
  viewModelOf(::OnboardingViewModel)

  viewModel {
    MyGamesViewModel(
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      OnlineGoApplication.instance.analytics,
      get(),
      get(),
      get()
    )
  }

  viewModelOf(::GameViewModel)

  viewModel {
    FaceToFaceViewModel(
      get(),
      PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext),
      OnlineGoApplication.instance.analytics,
      FirebaseCrashlytics.getInstance()
    )
  }

  viewModelOf(::SettingsViewModel)
}

private val espressoModule = module {
  single<CountingIdlingResource> { NOOPIdlingResource() }
}

private val playStoreModule = module {
  single { PlayStoreService(get()) }
}

val allKoinModules = listOf(
  repositoriesModule,
  serverConnectionModule,
  databaseModule,
  viewModelsModule,
  useCasesModule,
  espressoModule,
  playStoreModule
)
