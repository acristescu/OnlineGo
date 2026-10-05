package io.zenandroid.onlinego.data.repositories

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import io.zenandroid.onlinego.data.ogs.OGSCookieStore
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.PersistenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext.get

class UserSessionRepository(
  private val appCoroutineScope: CoroutineScope,
  private val cookieStore: OGSCookieStore,
  private val persistenceManager: PersistenceManager,
  private val context: Context,
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

  init {
    appCoroutineScope.launch(Dispatchers.IO) {
      uiConfig = persistenceManager.getUIConfig()
      userIdValue?.toString()?.let(CrashReporter::setUserId)
      userIdValue?.let {
        _userId.tryEmit(it)
      }
      _loginStatus.tryEmit(if (isLoggedIn()) LoginStatus.LoggedIn(userIdValue!!) else LoginStatus.LoggedOut)
    }
  }

  fun storeUIConfig(uiConfig: UIConfig) {
    this.uiConfig = uiConfig
    uiConfigTimestamp = System.currentTimeMillis()
    CrashReporter.setUserId(uiConfig.user?.id.toString())
    persistenceManager.storeUIConfig(uiConfig)
    userIdValue?.let {
      _userId.tryEmit(it)
    }
    _loginStatus.tryEmit(if (isLoggedIn()) LoginStatus.LoggedIn(userIdValue!!) else LoginStatus.LoggedOut)
    socketService.resendAuth()
  }

  fun requiresUIConfigRefresh(): Boolean {
    if (uiConfigTimestamp == null) {
      uiConfigTimestamp = persistenceManager.getUIConfigTimestamp()
    }
    return uiConfig?.user_jwt == null || uiConfigTimestamp!! < System.currentTimeMillis() - 1000 * 60 * 60
  }
  fun isLoggedIn() = uiConfig != null && cookieStore.sessionId != null

  fun logOut() {
    CrashReporter.sendUnsentReports()
    uiConfig = null
    _loginStatus.tryEmit(LoginStatus.LoggedOut)
    (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
  }

  suspend fun deleteAccount(password: String) {
    restService.deleteMyAccount(password)
  }
}

sealed interface LoginStatus {
  class LoggedIn(val userId: Long) : LoginStatus
  object LoggedOut : LoginStatus
}