package io.zenandroid.onlinego

import android.app.Application
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.gamelogic.RulesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level


/**
 * Created by alex on 04/11/2017.
 */
class OnlineGoApplication : Application() {

    companion object {
        lateinit var instance: OnlineGoApplication
    }

    val analytics by lazy { FirebaseAnalytics.getInstance(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@OnlineGoApplication)

            modules(allKoinModules)
        }
        applicationScope.launch(Dispatchers.IO) {
            Log.d("AppInit", "Pre-warming network stack on ${Thread.currentThread().name}")
            try {
                // Eagerly resolve the dependencies that are slow
                getKoin().get<OGSRestService>()
                getKoin().get<OGSWebSocketService>()
                Log.d("AppInit", "Network stack pre-warmed")
            } catch (e: Exception) {
                Log.e("AppInit", "Error pre-warming Koin dependencies", e)
            }
            RulesManager.coordinateToCell("A1")
            FirebaseCrashlytics.getInstance().log("Done pre-warming network stack")
        }

    }
}