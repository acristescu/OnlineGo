package io.zenandroid.onlinego

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.di.prewarmApp
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.CrashlyticsBreadcrumbWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level


/**
 * Created by alex on 04/11/2017.
 */
class OnlineGoApplication : Application(), Configuration.Provider {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()

    /**
     * Applies the language the user picked in the settings, so that strings pulled from the
     * application context (notifications, for instance) are localized too.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
      useAndroidSecureRandomForKtor()
        if (BuildConfig.DEBUG) {
            Logger.setLogWriters(platformLogWriter(), CrashlyticsBreadcrumbWriter())
        } else {
            Logger.setMinSeverity(Severity.Info)
            Logger.setLogWriters(CrashlyticsBreadcrumbWriter())
        }

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@OnlineGoApplication)

            modules(allKoinModules)
        }
        applicationScope.launch(Dispatchers.IO) {
          prewarmApp(this@OnlineGoApplication, resources.displayMetrics.density)
        }
    }

  private fun useAndroidSecureRandomForKtor() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      System.setProperty("io.ktor.random.secure.random.provider", "SHA1PRNG")
    }
  }
}
