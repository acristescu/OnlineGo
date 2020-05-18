package io.zenandroid.onlinego

import android.app.Application
import androidx.room.Room
import android.os.Build
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.core.provider.FontRequest
import android.util.Log
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Completable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.Database
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.ui.views.BoardView
import java.io.IOException
import java.net.SocketException


/**
 * Created by alex on 04/11/2017.
 */
class OnlineGoApplication : Application() {

    companion object {
        lateinit var instance: OnlineGoApplication
    }

    val db by lazy {
        Room.databaseBuilder(this, Database::class.java, "database.db")
                .fallbackToDestructiveMigration()
                .build()
    }
    val analytics by lazy { FirebaseAnalytics.getInstance(this) }
    val chatRepository by lazy { ChatRepository(db.gameDao()) }

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        instance = this

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
                Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            if (e is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            Log.w("OnlineGoApplication", "Undeliverable exception received, not sure what to do", e)
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

        Completable.create { BoardView.preloadResources(resources) }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}