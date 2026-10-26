package io.zenandroid.onlinego.di

import androidx.room.RoomDatabase
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.Url
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.Glicko2HistoryConverterFactory
import io.zenandroid.onlinego.data.ogs.OGSCookieStore
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.OGS_BASE_URL
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
import io.zenandroid.onlinego.data.repositories.SocketConnectedRepository
import io.zenandroid.onlinego.data.repositories.SocketDebugRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeViewModel
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceViewModel
import io.zenandroid.onlinego.ui.screens.game.GameViewModel
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerViewModel
import io.zenandroid.onlinego.ui.screens.learn.LearnViewModel
import io.zenandroid.onlinego.ui.screens.localai.AiGameViewModel
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeViewModel
import io.zenandroid.onlinego.ui.screens.newchallenge.SelectOpponentViewModel
import io.zenandroid.onlinego.ui.screens.puzzle.directory.PuzzleDirectoryViewModel
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoViewModel
import io.zenandroid.onlinego.ui.screens.socketdebug.SocketDebugViewModel
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialViewModel
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.appJson
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val SETTINGS_DATA_STORE = named("settings")
val REVIEW_PROMPT_DATA_STORE = named("review_prompt")
val WHATS_NEW_DATA_STORE = named("whats_new")

val sharedModule = module {
  single { appJson }

  single {
    OGSCookieStore(
      persistence = get(),
      host = Url(OGS_BASE_URL).host,
    )
  }

  single {
    HttpClient(get<HttpClientEngine>()) {
      configureOGSClient(
        jsonFormat = get(),
        cookiesStorage = get<OGSCookieStore>(),
        baseUrl = OGS_BASE_URL,
      )
      install(WebSockets) {
        pingIntervalMillis = 15_000
      }
    }
  }

  single {
    Ktorfit.Builder()
      .baseUrl(OGS_BASE_URL)
      .httpClient(get<HttpClient>())
      .converterFactories(Glicko2HistoryConverterFactory)
      .build()
      .createOGSRestAPI()
  }

  singleOf(::OGSRestService)
  singleOf(::OGSWebSocketService)

  single {
    get<RoomDatabase.Builder<Database>>()
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()
  }
  single { get<Database>().gameDao() }
  single { get<Database>().puzzleDao() }

  single<List<SocketConnectedRepository>> {
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
  singleOf(::ClockDriftRepository)
  singleOf(::SocketDebugRepository)
  singleOf(::UserSessionRepository)
  singleOf(::TutorialsRepository)
  singleOf(::PersistenceManager)
  single { SettingsRepository(get(SETTINGS_DATA_STORE), get()) }
  single { ReviewPromptRepository(get(REVIEW_PROMPT_DATA_STORE), get(), get()) }
  single { WhatsNewUtils(get(WHATS_NEW_DATA_STORE)) }

  singleOf(::GetUserStatsUseCase)

  viewModelOf(::LearnViewModel)
  viewModelOf(::PuzzleDirectoryViewModel)
  viewModelOf(::TsumegoViewModel)
  viewModelOf(::NewAutomatchChallengeViewModel)
  viewModelOf(::SocketDebugViewModel)
  viewModelOf(::TutorialViewModel)
  viewModelOf(::FaceToFaceViewModel)
  viewModelOf(::MyGamesViewModel)
  viewModelOf(::NewChallengeViewModel)
  viewModelOf(::SelectOpponentViewModel)
  viewModelOf(::GameViewModel)
  viewModelOf(::AiGameViewModel)
  viewModelOf(::JosekiExplorerViewModel)
  viewModelOf(::StatsViewModel)
}
