package io.zenandroid.onlinego.di

import io.zenandroid.onlinego.data.repositories.AppDataEraser
import io.zenandroid.onlinego.data.repositories.TutorialsSource
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.GameNotificationCanceller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

fun initKoin(
  analytics: Analytics,
  tutorialsSource: TutorialsSource,
  gameNotificationCanceller: GameNotificationCanceller,
) {
  startKoin {
    modules(
      sharedModule,
      iosPlatformModule,
      module {
        single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single { analytics }
        single { tutorialsSource }
        single { gameNotificationCanceller }
        single<AppDataEraser> {
          IosAppDataEraser(get(), get(), get()) {
            stopKoin()
            initKoin(analytics, tutorialsSource, gameNotificationCanceller)
          }
        }
      },
    )
  }
}
