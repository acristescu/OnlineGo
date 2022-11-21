package io.zenandroid.onlinego

import android.app.Application
import android.os.Build
import androidx.core.provider.FontRequest
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.zenandroid.onlinego.di.*
import org.koin.android.ext.koin.androidContext
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
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

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        startKoin {
            androidContext(this@OnlineGoApplication)

            modules(allKoinModules)
        }

        instance = this
        AndroidThreeTen.init(this)

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

        val settingsRepository: SettingsRepository = GlobalContext.get().get()
        when (settingsRepository.appTheme) {
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                val defaultNightMode = AppCompatDelegate.getDefaultNightMode()
                if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                    //special case handling the "unspecified" night mode, the one we get e.g. in
                    //case of battery saving, which doesn't trigger any night mode decision at startup.
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }
            }
        }

        val config: EmojiCompat.Config
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)
        config = FontRequestEmojiCompatConfig(applicationContext, fontRequest)
                .setReplaceAll(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
        EmojiCompat.init(config)
    }
}