package io.zenandroid.onlinego.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.zenandroid.onlinego.ai.AnalysisEngine
import io.zenandroid.onlinego.ai.UnavailableAnalysisEngine
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.db.DatabaseTest
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.db.PuzzleDao
import io.zenandroid.onlinego.data.model.AppLanguage
import io.zenandroid.onlinego.data.ogs.OGSRestAPI
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.SessionCookiePersistence
import io.zenandroid.onlinego.data.ogs.StoredCookie
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AppDataEraser
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
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.composables.BundleSoundLoader
import io.zenandroid.onlinego.ui.composables.SoundLoader
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeViewModel
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
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.AppLanguageSetting
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import io.zenandroid.onlinego.utils.KeyValueStore
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.WhatsNewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedModuleTest {

  @AfterTest
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun everySharedDefinitionResolvesFromThePlatformContract() = runTest {
    startKoin { modules(sharedModule, stubPlatformModule(backgroundScope)) }
    val koin = getKoin()

    koin.get<OGSRestAPI>()
    koin.get<OGSRestService>()
    koin.get<OGSWebSocketService>()
    koin.get<GameDao>()
    koin.get<PuzzleDao>()
    koin.get<ActiveGamesRepository>()
    koin.get<AutomatchRepository>()
    koin.get<BotsRepository>()
    koin.get<ChallengesRepository>()
    koin.get<ChatRepository>()
    koin.get<FinishedGamesRepository>()
    koin.get<JosekiRepository>()
    koin.get<PuzzleRepository>()
    koin.get<PlayersRepository>()
    koin.get<ServerNotificationsRepository>()
    koin.get<ClockDriftRepository>()
    koin.get<SocketDebugRepository>()
    koin.get<UserSessionRepository>()
    koin.get<TutorialsRepository>()
    koin.get<PersistenceManager>()
    koin.get<SettingsRepository>()
    koin.get<ReviewPromptRepository>()
    koin.get<GetUserStatsUseCase>()
    koin.get<LearnViewModel>()
    koin.get<PuzzleDirectoryViewModel>()
    koin.get<TsumegoViewModel> {
      parametersOf(
        SavedStateHandle(
          mapOf(
            "collectionId" to 1L,
            "puzzleId" to 2L
          )
        )
      )
    }
    koin.get<NewAutomatchChallengeViewModel>()
    koin.get<SocketDebugViewModel>()
    koin.get<MyGamesViewModel>()
    koin.get<NewChallengeViewModel>()
    koin.get<SelectOpponentViewModel>()
    koin.get<GameDao>().insertAllGames(listOf(DatabaseTest.game))
    koin.get<GameViewModel> {
      parametersOf(
        SavedStateHandle(
          mapOf<String, Any>(
            "gameId" to 1L,
            "gameWidth" to 19,
            "gameHeight" to 19
          )
        )
      )
    }
      .viewModelScope.cancel()
    koin.get<AiGameViewModel>().viewModelScope.cancel()
    koin.get<JosekiExplorerViewModel>().viewModelScope.cancel()
    koin.get<WhatsNewUtils>()
    assertEquals(9, koin.get<List<SocketConnectedRepository>>().size)
  }
}

internal fun stubPlatformModule(scope: CoroutineScope, tutorialsJson: String = "[]") = module {
  single { scope }
  single<HttpClientEngine> { MockEngine { respondError(HttpStatusCode.ServiceUnavailable) } }
  single<SessionCookiePersistence> {
    object : SessionCookiePersistence {
      override fun load() = emptyMap<String, StoredCookie>()
      override fun save(cookies: Map<String, StoredCookie>) {}
    }
  }
  single<KeyValueStore> {
    object : KeyValueStore {
      override fun getString(key: String): String? = null
      override fun getLong(key: String, default: Long) = default
      override fun putString(key: String, value: String) {}
      override fun putLong(key: String, value: Long) {}
    }
  }
  single(SETTINGS_DATA_STORE) { tempDataStore(scope) }
  single(REVIEW_PROMPT_DATA_STORE) { tempDataStore(scope) }
  single(WHATS_NEW_DATA_STORE) { tempDataStore(scope) }
  single { Room.inMemoryDatabaseBuilder<Database>().setDriver(BundledSQLiteDriver()) }
  single<Analytics> {
    object : Analytics {
      override fun logEvent(name: String, params: Map<String, String?>) {}
      override fun logScreenView(screenName: String) {}
    }
  }
  single { AppDataEraser {} }
  single { TutorialsSource { tutorialsJson } }
  single { GameNotificationCanceller {} }
  single<SoundLoader> { BundleSoundLoader }
  single<AnalysisEngine> { UnavailableAnalysisEngine }
  single<AppLanguageSetting> {
    object : AppLanguageSetting {
      override val language = MutableStateFlow(AppLanguage.DEFAULT)
      override fun refresh() {}
      override fun setLanguage(language: AppLanguage) {
        this.language.value = language
      }
    }
  }
}

private fun tempDataStore(scope: CoroutineScope) =
  PreferenceDataStoreFactory.createWithPath(scope = scope) {
    FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "koin-${Random.nextLong().toULong()}.preferences_pb"
  }
