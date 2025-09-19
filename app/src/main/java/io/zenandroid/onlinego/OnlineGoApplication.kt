package io.zenandroid.onlinego

import android.app.Application
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.zenandroid.onlinego.di.allKoinModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.IOException
import java.net.SocketException


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
//        applicationScope.launch(Dispatchers.IO) {
//            Log.d("AppInit", "Pre-warming network stack on ${Thread.currentThread().name}")
//            try {
//                // Eagerly resolve the dependencies that are slow
//                getKoin().get<OGSRestService>()
//                getKoin().get<OGSWebSocketService>()
//                Log.d("AppInit", "Network stack pre-warmed")
//            } catch (e: Exception) {
//                Log.e("AppInit", "Error pre-warming Koin dependencies", e)
//            }
//            RulesManager.coordinateToCell("A1")
//            FirebaseCrashlytics.getInstance().log("Done pre-warming network stack")
//        }

        RxJavaPlugins.setErrorHandler {
            val e = if (it is UndeliverableException) it.cause else it
            if (e is IOException || e is SocketException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (e is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if (e is NullPointerException || e is IllegalArgumentException) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            if (e is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            Log.w("OnlineGoApplication", "Undeliverable exception received, not sure what to do", e)
        }
    }
}