package io.zenandroid.onlinego.di

import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.HTTPConnectionFactory
import io.zenandroid.onlinego.data.ogs.OGSRestAPI
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.ui.screens.joseki.*
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchMiddleware
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerReducer
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerState
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*

private val repositoriesModule = module {
    single {
        listOf(
                get<ActiveGamesRepository>(),
                get<AutomatchRepository>(),
                get<BotsRepository>(),
                get<ChallengesRepository>(),
                get<FinishedGamesRepository>(),
                get<ServerNotificationsRepository>()
        )
    }

    single { ActiveGamesRepository(get(), get(), get(), get()) }
    single { AutomatchRepository(get()) }
    single { BotsRepository(get()) }
    single { ChallengesRepository(get(), get(), get()) }
    single { ChatRepository(get()) }
    single { FinishedGamesRepository(get(), get(), get(), get()) }
    single { JosekiRepository(get(), get()) }
    single { PlayersRepository(get(), get(), get()) }
    single { ServerNotificationsRepository(get()) }
    single { SettingsRepository() }
    single { UserSessionRepository() }
}

private val serverConnectionModule = module {

    single { HTTPConnectionFactory(get()) }
    single { get<HTTPConnectionFactory>().buildConnection() }

    single {
        Retrofit.Builder()
            .baseUrl("https://online-go.com/")
            .client(get())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
            .create(OGSRestAPI::class.java)
    }

    single {
        Moshi.Builder()
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    single { OGSRestService(get(), get(), get()) }
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
                                AnalyticsMiddleware()
                        ),
                        JosekiExplorerState()
                )
        )
    }
}

val allKoinModules = listOf(
        repositoriesModule,
        serverConnectionModule,
        databaseModule,
        viewModelsModule
)
