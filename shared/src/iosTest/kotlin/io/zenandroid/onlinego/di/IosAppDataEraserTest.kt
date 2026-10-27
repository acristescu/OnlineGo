package io.zenandroid.onlinego.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.ogs.SessionCookiePersistence
import io.zenandroid.onlinego.data.ogs.StoredCookie
import io.zenandroid.onlinego.data.repositories.AppDataEraser
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import io.zenandroid.onlinego.utils.KeyValueStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.koin.mp.KoinPlatform.getKoin
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosAppDataEraserTest {

  @AfterTest
  fun tearDown() {
    val koin = KoinPlatform.getKoinOrNull() ?: return
    runBlocking { koin.get<CoroutineScope>().coroutineContext.job.cancelAndJoin() }
    koin.get<Database>().close()
    stopKoin()
  }

  @Test
  fun resettingWipesStoredDataAndStartsAFreshGraph() = runTest {
    startKoin()
    val old = getKoin()
    val probe = stringPreferencesKey("probe")
    old.get<KeyValueStore>().putString("probe", "logged in")
    old.get<SessionCookiePersistence>()
      .save(mapOf("sessionid" to StoredCookie("secret", Long.MAX_VALUE)))
    old.get<DataStore<Preferences>>(SETTINGS_DATA_STORE).edit { it[probe] = "dark" }
    val oldScope = old.get<CoroutineScope>()

    (old.get<AppDataEraser>() as IosAppDataEraser).reset()

    val new = getKoin()
    assertNotSame(old, new)
    assertFalse(oldScope.isActive)
    assertNull(new.get<KeyValueStore>().getString("probe"))
    assertTrue(new.get<SessionCookiePersistence>().load().isEmpty())
    assertNull(new.get<DataStore<Preferences>>(SETTINGS_DATA_STORE).data.first()[probe])
  }

  @OptIn(ExperimentalForeignApi::class)
  @Test
  fun erasingRemovesEveryFileAndTheGivenSuitesOnly() {
    val directory = NSTemporaryDirectory() + NSUUID().UUIDString
    NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
    listOf("database.db", "database.db-wal", "settings.preferences_pb").forEach { name ->
      (name as NSString).writeToFile("$directory/$name", true, NSUTF8StringEncoding, null)
    }
    NSUserDefaults(suiteName = "eraser_test_suite").setObject("value", "key")
    NSUserDefaults.standardUserDefaults.setObject("kept", "eraser_test_standard_key")

    eraseStoredData(directory, listOf("eraser_test_suite"))

    assertEquals(
      emptyList<Any?>(),
      NSFileManager.defaultManager.contentsOfDirectoryAtPath(directory, null)
    )
    assertNull(NSUserDefaults(suiteName = "eraser_test_suite").stringForKey("key"))
    assertEquals(
      "kept",
      NSUserDefaults.standardUserDefaults.stringForKey("eraser_test_standard_key")
    )
    NSUserDefaults.standardUserDefaults.removeObjectForKey("eraser_test_standard_key")
  }

  private fun startKoin() = initKoin(
    analytics = object : Analytics {
      override fun logEvent(name: String, params: Map<String, String?>) {}
      override fun logScreenView(screenName: String) {}
    },
    tutorialsSource = TutorialsSource { "[]" },
    gameNotificationCanceller = GameNotificationCanceller {},
  )
}
