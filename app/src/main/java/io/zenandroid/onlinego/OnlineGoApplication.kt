package io.zenandroid.onlinego

import android.app.Application
import android.content.Context
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.google.firebase.analytics.FirebaseAnalytics
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.mygames.composables.HEADER_AVATAR_SIZE
import io.zenandroid.onlinego.ui.screens.mygames.composables.HEADER_AVATAR_URL_WIDTH
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.CrashlyticsBreadcrumbWriter
import io.zenandroid.onlinego.utils.processGravatarURL
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

    /**
     * Applies the language the user picked in the settings, so that strings pulled from the
     * application context (notifications, for instance) are localized too.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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
            Logger.d(tag = "AppInit") { "Pre-warming network stack on ${Thread.currentThread().name}" }
            try {
                // Eagerly resolve the dependencies that are slow
                getKoin().get<OGSRestService>()
                getKoin().get<OGSWebSocketService>()
                preloadOwnAvatar()
                Logger.d(tag = "AppInit") { "Network stack pre-warmed" }
            } catch (e: Exception) {
                Logger.e("Error pre-warming Koin dependencies", e, "AppInit")
            }
            RulesManager.coordinateToCell("A1")
            Logger.i("Done pre-warming network stack", tag = "AppInit")
        }

    }

    /**
     * Warms the image caches so the home screen header renders the avatar on its first frame
     * rather than showing the placeholder and swapping. Mirrors the two sizes
     * [HomeScreenHeader][io.zenandroid.onlinego.ui.screens.mygames.composables.HomeScreenHeader]
     * uses, so the resulting memory cache entry is the one it will look up.
     */
    private fun preloadOwnAvatar() {
        val icon = getKoin().get<UserSessionRepository>().uiConfig?.user?.icon ?: return
        val density = resources.displayMetrics.density
        val urlWidth = (HEADER_AVATAR_URL_WIDTH.value * density).toInt()
        val displaySize = (HEADER_AVATAR_SIZE.value * density).toInt()
        SingletonImageLoader.get(this).enqueue(
            ImageRequest.Builder(this)
                .data(processGravatarURL(icon, urlWidth))
                .size(displaySize)
                .build()
        )
    }
}