package io.zenandroid.onlinego.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.zenandroid.onlinego.data.repositories.AppDataEraser
import io.zenandroid.onlinego.data.repositories.PushToken
import io.zenandroid.onlinego.data.repositories.PushTokenProvider
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAtomicApi::class)
class LogoutUnregistersPushTest {

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @AfterTest
  fun tearDown() {
    stopKoin()
    appScope.cancel()
  }

  @Test
  fun logoutUnregistersThePushTokenBeforeErasingAppData() = runTest {
    val events = AtomicReference(emptyList<String>())
    val erased = CompletableDeferred<Unit>()
    startKoin {
      modules(
        sharedModule,
        stubPlatformModule(appScope),
        module {
          single<HttpClientEngine> {
            MockEngine { request ->
              events.update { it + "${request.method.value} ${request.url.encodedPathAndQuery}" }
              respond(content = "", status = HttpStatusCode.OK)
            }
          }
          single {
            PushTokenProvider {
              PushToken(
                value = "device-token",
                fcmMessageType = "data"
              )
            }
          }
          single {
            AppDataEraser {
              events.update { it + "erase" }
              erased.complete(Unit)
            }
          }
        },
      )
    }

    getKoin().get<UserSessionRepository>().logOut()
    withContext(Dispatchers.Default) { withTimeout(10.seconds) { erased.await() } }

    assertEquals(
      listOf("DELETE /api/v1/me/push_devices?token=device-token", "erase"),
      events.load()
    )
  }
}

@OptIn(ExperimentalAtomicApi::class)
private fun <T> AtomicReference<T>.update(transform: (T) -> T) {
  while (true) {
    val current = load()
    if (compareAndSet(current, transform(current))) return
  }
}
