package io.zenandroid.onlinego.di

import io.ktor.client.HttpClient
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.repositories.AppDataEraser
import io.zenandroid.onlinego.ui.screens.main.appSession
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import kotlin.concurrent.atomics.AtomicBoolean

internal class IosAppDataEraser(
  private val applicationScope: CoroutineScope,
  private val httpClient: HttpClient,
  private val database: Database,
  private val restartKoin: () -> Unit,
) : AppDataEraser {
  private val started = AtomicBoolean(false)

  override fun clearAppData() {
    if (!started.compareAndSet(expectedValue = false, newValue = true)) return
    CoroutineScope(Dispatchers.Main).launch {
      appSession.restart(::reset)
    }
  }

  suspend fun reset() {
    applicationScope.coroutineContext.job.cancelAndJoin()
    httpClient.close()
    database.close()
    eraseStoredData(
      directory = applicationSupportDirectory(),
      defaultsSuites = listOf(SESSION_DEFAULTS_SUITE, LOGIN_DEFAULTS_SUITE),
    )
    restartKoin()
  }
}

@OptIn(ExperimentalForeignApi::class)
internal fun eraseStoredData(directory: String, defaultsSuites: List<String>) {
  val fileManager = NSFileManager.defaultManager
  fileManager.contentsOfDirectoryAtPath(directory, error = null)?.forEach { name ->
    fileManager.removeItemAtPath("$directory/$name", error = null)
  }
  defaultsSuites.forEach { NSUserDefaults.standardUserDefaults.removePersistentDomainForName(it) }
}
