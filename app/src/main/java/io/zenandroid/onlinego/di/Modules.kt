package io.zenandroid.onlinego.di

import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.*
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.ui.screens.game.GameViewModel
import io.zenandroid.onlinego.ui.screens.joseki.*
import io.zenandroid.onlinego.ui.screens.learn.LearnViewModel
import io.zenandroid.onlinego.ui.screens.localai.AiGameReducer
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.zenandroid.onlinego.ui.screens.localai.AiGameViewModel
import io.zenandroid.onlinego.ui.screens.localai.middlewares.*
import io.zenandroid.onlinego.ui.screens.localai.middlewares.AnalyticsMiddleware
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchMiddleware
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerReducer
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerState
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerViewModel
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialViewModel
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.CustomConverterFactory
import io.zenandroid.onlinego.utils.NOOPIdlingResource
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.threeten.bp.Instant
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

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
            .baseUrl("https://online-go.com/")
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

    viewModel {
        LearnViewModel(get())
    }

    viewModel {
        TutorialViewModel(get())
    }

    viewModel {
        OnboardingViewModel(get(), get())
    }

    viewModel {
        MyGamesViewModel(get(), get(), get(), get(), get(), get(), get(), get(), OnlineGoApplication.instance.analytics, get(), get(), get())
    }

    viewModel {
        GameViewModel(get(), get(), get(), get(), get(), get(), get())
    }
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
