package io.zenandroid.onlinego.data.repositories

import com.crashlytics.android.Crashlytics
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.utils.PersistenceManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.core.context.KoinContextHandler.get

class UserSessionRepository {
    // Note: Can't use constructor injection here because it will create a dependency loop and
    // Koin will throw a fit (at runtime)
    private val socketService: OGSWebSocketService by get().inject()

    var uiConfig: UIConfig? = null
        private set

    val userId: Long?
        get() = uiConfig?.user?.id

    val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(OnlineGoApplication.instance))

    init {
        uiConfig = PersistenceManager.getUIConfig()
        userId?.toString()?.let(Crashlytics::setUserIdentifier)
    }

    fun storeUIConfig(uiConfig: UIConfig) {
        this.uiConfig = uiConfig
        socketService.authSent = false
        Crashlytics.setUserIdentifier(uiConfig.user.id.toString())
        PersistenceManager.storeUIConfig(uiConfig)
    }

    fun requiresUIConfigRefresh(): Boolean {
        if(uiConfig?.user_jwt == null) {
            return true
        }

        return false
    }

    fun isLoggedIn() =
            (uiConfig != null) &&
                    cookieJar.loadForRequest("https://online-go.com/".toHttpUrlOrNull()!!)
                            .any { it.name == "sessionid" }

    fun logOut() {
        uiConfig = null
        PersistenceManager.deleteUIConfig()
        cookieJar.clear()
        socketService.disconnect()
        SynchronizeGamesWork.unschedule()
    }

}