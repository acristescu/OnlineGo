package io.zenandroid.onlinego.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.zenandroid.onlinego.ai.AnalysisEngine
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.db.PuzzleDao
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.ReviewPromptRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.composables.SoundLoader
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import io.zenandroid.onlinego.utils.IosReviewPromptManager
import io.zenandroid.onlinego.utils.PersistenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test

class IosPlatformModuleTest {

  @AfterTest
  fun tearDown() {
    val koin = KoinPlatform.getKoinOrNull() ?: return
    runBlocking { koin.get<CoroutineScope>().coroutineContext.job.cancelAndJoin() }
    koin.get<Database>().close()
    stopKoin()
  }

  @Test
  fun theRealPlatformBindingsResolveAndOpen() = runTest {
    initKoin(
      analytics = object : Analytics {
        override fun logEvent(name: String, params: Map<String, String?>) {}
        override fun logScreenView(screenName: String) {}
      },
      tutorialsSource = TutorialsSource { "[]" },
      gameNotificationCanceller = GameNotificationCanceller {},
    )
    val koin = getKoin()

    koin.get<OGSRestService>()
    koin.get<OGSWebSocketService>()
    koin.get<UserSessionRepository>()
    koin.get<SoundLoader>()
    koin.get<AnalysisEngine>()
    koin.get<PersistenceManager>()
    koin.get<SettingsRepository>()
    koin.get<ReviewPromptRepository>()
    koin.get<IosReviewPromptManager>()
    koin.get<PuzzleDao>().getPuzzleCollectionCount()
    koin.get<DataStore<Preferences>>(SETTINGS_DATA_STORE).data.first()
    koin.get<DataStore<Preferences>>(REVIEW_PROMPT_DATA_STORE).data.first()
  }
}
