package io.zenandroid.onlinego.data.repositories

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.PersistenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

  private val userIdSubject = BehaviorSubject.create<Long>()
  val userIdObservable: Observable<Long> = userIdSubject.hide().distinctUntilChanged()

  private val loggedInSubject = BehaviorSubject.create<LoginStatus>()
  val loggedInObservable: Observable<LoginStatus> = loggedInSubject.hide().distinctUntilChanged()

  var uiConfig: UIConfig? = null
    private set

  private val userId: Long?
    get() = uiConfig?.user?.id
//        get() = 126739L

  val cookieJar =
    PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(OnlineGoApplication.instance))

  init {
    appCoroutineScope.launch(Dispatchers.IO) {
      uiConfig = PersistenceManager.getUIConfig()
      userId?.toString()?.let(FirebaseCrashlytics.getInstance()::setUserId)
      userId?.let {
        userIdSubject.onNext(it)
      }
      loggedInSubject.onNext(if (isLoggedIn()) LoginStatus.LoggedIn(userId!!) else LoginStatus.LoggedOut)
    }
  }

  fun storeUIConfig(uiConfig: UIConfig) {
    this.uiConfig = uiConfig
    socketService.resendAuth()
    FirebaseCrashlytics.getInstance().setUserId(uiConfig.user?.id.toString())
    PersistenceManager.storeUIConfig(uiConfig)
    userId?.let {
      userIdSubject.onNext(it)
    }
    loggedInSubject.onNext(if (isLoggedIn()) LoginStatus.LoggedIn(userId!!) else LoginStatus.LoggedOut)
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
    loggedInSubject.onNext(if (isLoggedIn()) LoginStatus.LoggedIn(userId!!) else LoginStatus.LoggedOut)
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