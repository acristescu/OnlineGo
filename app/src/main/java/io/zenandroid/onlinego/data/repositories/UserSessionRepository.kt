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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.core.context.GlobalContext.get

class UserSessionRepository(
  private val appCoroutineScope: CoroutineScope
) {
  // Note: Can't use constructor injection here because it will create a dependency loop and
  // Koin will throw a fit (at runtime)
  private val socketService: OGSWebSocketService by get().inject()
  private val restService: OGSRestService by get().inject()

  private val _userId = MutableSharedFlow<Long?>(replay = 1)
  val userId: SharedFlow<Long?> = _userId.asSharedFlow()

  private val _loginStatus = MutableSharedFlow<LoginStatus>(replay = 1)
  val loginStatus: SharedFlow<LoginStatus> = _loginStatus.asSharedFlow()

  var uiConfig: UIConfig? = null
    private set
  private var uiConfigTimestamp: Long? = null

  private val userIdValue: Long?
    get() = uiConfig?.user?.id
//        get() = 126739L

  val cookieJar =
    PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(OnlineGoApplication.instance))

  init {
    appCoroutineScope.launch(Dispatchers.IO) {
      uiConfig = PersistenceManager.getUIConfig()
      userIdValue?.toString()?.let(FirebaseCrashlytics.getInstance()::setUserId)
      userIdValue?.let {
        _userId.tryEmit(it)
      }
      _loginStatus.tryEmit(if (isLoggedIn()) LoginStatus.LoggedIn(userIdValue!!) else LoginStatus.LoggedOut)
    }
  }

  fun storeUIConfig(uiConfig: UIConfig) {
    this.uiConfig = uiConfig
    uiConfigTimestamp = System.currentTimeMillis()
    FirebaseCrashlytics.getInstance().setUserId(uiConfig.user?.id.toString())
    PersistenceManager.storeUIConfig(uiConfig)
    userIdValue?.let {
      _userId.tryEmit(it)
    }
    _loginStatus.tryEmit(if (isLoggedIn()) LoginStatus.LoggedIn(userIdValue!!) else LoginStatus.LoggedOut)
    socketService.resendAuth()
  }

  fun requiresUIConfigRefresh(): Boolean {
    if (uiConfigTimestamp == null) {
      uiConfigTimestamp = PersistenceManager.getUIConfigTimestamp()
    }
    return uiConfig?.user_jwt == null || uiConfigTimestamp!! < System.currentTimeMillis() - 1000 * 60 * 60
  }
  fun isLoggedIn() =
    (uiConfig != null) &&
        cookieJar.loadForRequest(BuildConfig.BASE_URL.toHttpUrlOrNull()!!)
          .any { it.name == "sessionid" }

  fun logOut() {
    FirebaseCrashlytics.getInstance().sendUnsentReports()
    uiConfig = null
    _loginStatus.tryEmit(LoginStatus.LoggedOut)
    (OnlineGoApplication.instance.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
  }

  suspend fun deleteAccount(password: String) {
    restService.deleteMyAccount(password)
  }
}

sealed interface LoginStatus {
  class LoggedIn(val userId: Long) : LoginStatus
  object LoggedOut : LoginStatus
}