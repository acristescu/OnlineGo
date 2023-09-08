package io.zenandroid.onlinego.data.repositories

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.PersistenceManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.core.context.GlobalContext.get

class UserSessionRepository {
  // Note: Can't use constructor injection here because it will create a dependency loop and
  // Koin will throw a fit (at runtime)
  private val socketService: OGSWebSocketService by get().inject()
  private val restService: OGSRestService by get().inject()

  var uiConfig: UIConfig? = null
    private set

  val userId: Long?
    get() = uiConfig?.user?.id
//        get() = 126739L

  val cookieJar =
    PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(OnlineGoApplication.instance))

  init {
    uiConfig = PersistenceManager.getUIConfig()
    userId?.toString()?.let(FirebaseCrashlytics.getInstance()::setUserId)
  }

  fun storeUIConfig(uiConfig: UIConfig) {
    this.uiConfig = uiConfig
    socketService.resendAuth()
    FirebaseCrashlytics.getInstance().setUserId(uiConfig.user?.id.toString())
    PersistenceManager.storeUIConfig(uiConfig)
  }

  fun requiresUIConfigRefresh(): Boolean =
    uiConfig?.user_jwt == null

  fun isLoggedIn() =
    (uiConfig != null) &&
      cookieJar.loadForRequest(BuildConfig.BASE_URL.toHttpUrlOrNull()!!)
        .any { it.name == "sessionid" }

  fun logOut() {
    FirebaseCrashlytics.getInstance().sendUnsentReports()
    uiConfig = null
    (OnlineGoApplication.instance.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
  }

  suspend fun deleteAccount(password: String) {
    restService.deleteMyAccount(password)
  }
}