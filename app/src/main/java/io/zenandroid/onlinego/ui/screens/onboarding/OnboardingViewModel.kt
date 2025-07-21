package io.zenandroid.onlinego.ui.screens.onboarding

import android.os.Build
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.onboarding.Page.LoginPage
import io.zenandroid.onlinego.ui.screens.onboarding.Page.MultipleChoicePage
import io.zenandroid.onlinego.ui.screens.onboarding.Page.NotificationPermissionPage
import io.zenandroid.onlinego.ui.screens.onboarding.Page.OnboardingPage
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class OnboardingViewModel(
  val ogsRestService: OGSRestService,
  val ogsWebSocketService: OGSWebSocketService,
  val settingsRepository: SettingsRepository,
  val userSessionRepository: UserSessionRepository,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val pages = arrayOf(
    OnboardingPage(
      R.drawable.art_onboarding,
      "The Game of GO",
      "Go is a strategy board game for two players, in which the aim is to surround more territory than the opponent. The game was invented in China more than 2,500 years ago and is the oldest board game still played today. It is estimated that more than 46 million people know how to play.",
      "Continue"
    ),
    OnboardingPage(
      R.drawable.ic_board_transparent,
      "Online GO Android app",
      "This app started back in 2015 as a personal project by Alexandru Cristescu. It is now open-source, meaning the code is freely available for everybody to browse and modify. The app is and will always be free. Contributions are welcome. Find out how you can help on the project's GitHub page. If coding is not your thing, you can become a supporter by pledging a monthly contribution. Or, you can just enjoy the app for free, the choice is entirely yours!",
      "Continue"
    ),
    OnboardingPage(
      R.drawable.logo_ogs,
      "OGS Server",
      "The OGS Server is one of the most popular websites for playing GO online. The Online GO Android app uses the services provided by OGS in order to enable online play. While not associated with OGS, we do have their permission to use their services. A (free) OGS account is required in order to play. You can create one in the next step if you don't already have it.",
      "Link OGS account"
    ),
    MultipleChoicePage(
      "Do you already have an OGS (online-go.com) account?",
      listOf("Yes", "No")
    ),
    MultipleChoicePage(
      "What log in method do you want to use?",
      listOf("Google Sign-in", "Username and password", "Stay offline")
    ),
    LoginPage,
    NotificationPermissionPage(
      "Stay Updated",
      "We'd like to send you notifications when it's your turn to play, when games finish, or when you receive messages. You can always change this later in your device settings.",
      "Allow notifications",
      "Skip"
    )
  ).drop(if (savedStateHandle["initialPageArg"] as String? != null) 4 else 0)

  private val analytics = OnlineGoApplication.instance.analytics
  private val subscriptions = CompositeDisposable()

  init {
    userSessionRepository.loggedInObservable.firstOrError()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
      { loggedIn ->
        if (loggedIn is LoginStatus.LoggedIn) {
          //
          // This should only happen during the migration
          //
          viewModelScope.launch {
            settingsRepository.setHasCompletedOnboarding(true)
            _state.update { it.copy(onboardingDone = true) }
          }
        }
      },
      { recordException(it) }
    ).addToDisposable(subscriptions)
  }

  private val _state =
    MutableStateFlow(
      OnboardingState(
        currentPageIndex = 0,
        currentPage = pages[0],
        isExistingAccount = savedStateHandle["initialPageArg"] as String? == "login",
        totalPages = pages.size,
      )
    )
  val state: StateFlow<OnboardingState> = _state.asStateFlow()

  override fun onCleared() {
    subscriptions.clear()
    super.onCleared()
  }

  fun onAction(action: OnboardingAction) {
    when (action) {
      OnboardingAction.BackPressed -> {
        if (state.value.currentPageIndex != 0) {
          goToPage(state.value.currentPageIndex - 1)
        } else {
          _state.update { it.copy(finish = true) }
        }
      }

      OnboardingAction.ContinueClicked -> {
        goToPage(state.value.currentPageIndex + 1)
      }

      is OnboardingAction.AnswerSelected -> {
        when (state.value.currentPageIndex) { // FIXME, this is a mess
          3 -> _state.update { it.copy(isExistingAccount = action.answerIndex == 0) }
          4, 0 -> {
            if (action.answerIndex == 2) {
              _state.update { it.copy(showOfflineConfirmationDialog = true) }
              return
            } else {
              _state.update { it.copy(loginMethod = Page.LoginMethod.entries[action.answerIndex]) }
            }
          }
        }
        goToPage(state.value.currentPageIndex + 1)
      }

      OnboardingAction.ConfirmStayOffline -> {
        viewModelScope.launch {
          settingsRepository.setHasCompletedOnboarding(true)
          _state.update {
            it.copy(
              onboardingDone = true,
              showOfflineConfirmationDialog = false
            )
          }
        }
      }

      OnboardingAction.CancelStayOffline -> {
        _state.update {
          it.copy(showOfflineConfirmationDialog = false)
        }
      }

      is OnboardingAction.EmailChanged -> _state.update {
        it.copy(
          email = action.newEmail,
          logInButtonEnabled = shouldEnableLogInButton(
            action.newEmail,
            it.username,
            it.password,
            it.isExistingAccount
          )
        )
      }

      is OnboardingAction.PasswordChanged -> _state.update {
        it.copy(
          password = action.newPassword,
          logInButtonEnabled = shouldEnableLogInButton(
            it.email,
            it.username,
            action.newPassword,
            it.isExistingAccount
          )
        )
      }

      is OnboardingAction.UsernameChanged -> _state.update {
        it.copy(
          username = action.newUsername,
          logInButtonEnabled = shouldEnableLogInButton(
            it.email,
            action.newUsername,
            it.password,
            it.isExistingAccount
          )
        )
      }

      OnboardingAction.LoginPressed -> {
        onLoginClicked(state.value)
        _state.update { it.copy(loginProcessing = true) }
      }

      OnboardingAction.DialogDismissed -> _state.update { it.copy(loginErrorDialogText = null) }
      OnboardingAction.SocialPlatformLoginFailed -> _state.update {
        it.copy(
          loginProcessing = false,
          loginMethod = null,
          currentPageIndex = if (it.currentPageIndex == 0) 0 else it.currentPageIndex - 1,
          currentPage = if (it.currentPageIndex == 0) pages[0] else pages[it.currentPageIndex - 1],
        )
      }

      OnboardingAction.AllowNotificationsClicked -> {
        _state.update { it.copy(requestNotificationPermission = true) }
      }

      OnboardingAction.SkipNotificationsClicked, OnboardingAction.PermissionsGranted -> {
        viewModelScope.launch {
          settingsRepository.setHasCompletedOnboarding(true)
          _state.update { it.copy(onboardingDone = true) }
        }
      }
    }
  }

  private fun shouldEnableLogInButton(
    email: String,
    username: String,
    password: String,
    isExistingAccount: Boolean
  ) =
    (isExistingAccount || email.isNotBlank()) && username.isNotBlank() && password.isNotBlank()

  private fun goToPage(pageIndex: Int) {
    analytics.logEvent("oboarding_page_$pageIndex", null)
    _state.update {
      it.copy(
        currentPageIndex = pageIndex,
        currentPage = pages[pageIndex],
        logInButtonEnabled = shouldEnableLogInButton(
          it.email,
          it.username,
          it.password,
          it.isExistingAccount
        )
      )
    }
  }

  private fun onLoginClicked(state: OnboardingState) {
    FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "PASSWORD")
    if (state.isExistingAccount) {
      doLogin(state)
    } else {
      ogsRestService.createAccount(state.username.trim(), state.password, state.email.trim())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(this::onCreateAccountSuccess, this::onCreateAccountFailure)
        .addToDisposable(subscriptions)
    }
  }

  private fun doLogin(state: OnboardingState) {
    ogsRestService.login(state.username.trim(), state.password)
      .doOnComplete { ogsWebSocketService.ensureSocketConnected() }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .doOnComplete { analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null) }
      .subscribe(this::onLoginSuccess, this::onPasswordLoginFailure)
      .addToDisposable(subscriptions)
  }

  private fun onCreateAccountSuccess() {
    FirebaseCrashlytics.getInstance().setCustomKey("NEW_ACCOUNT", true)
    analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
    doLogin(_state.value)
  }

  private fun onLoginSuccess() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      goToPage(state.value.currentPageIndex + 1)
    } else {
      // For older Android versions, notifications are allowed by default
      viewModelScope.launch {
        settingsRepository.setHasCompletedOnboarding(true)
        _state.update { it.copy(onboardingDone = true) }
      }
    }
  }

  private fun onPasswordLoginFailure(t: Throwable) {
    Log.e(OnboardingViewModel::class.java.simpleName, t.message, t)
    if ((t as? HttpException)?.code() in arrayOf(401, 403)) {
      _state.update {
        it.copy(
          loginProcessing = false,
          loginErrorDialogText = "Invalid username or password"
        )
      }
    } else {
      recordException(t)
      _state.update {
        it.copy(
          loginProcessing = false,
          loginErrorDialogText = "Login failed. Debug info: '${t.message}'"
        )
      }
    }
  }

  private fun onCreateAccountFailure(t: Throwable) {
    Log.e(OnboardingViewModel::class.java.simpleName, t.message, t)
    if (t is HttpException && t.response()?.errorBody() != null) {
      try {
        val error = JSONObject(t.response()?.errorBody()!!.string())["error"].toString()
        _state.update { it.copy(loginProcessing = false, loginErrorDialogText = error) }
      } catch (e: Exception) {
        Log.e(
          OnboardingViewModel::class.java.simpleName,
          "Can't parse error: ${t.response()?.errorBody()?.string()}"
        )
        _state.update {
          it.copy(
            loginProcessing = false,
            loginErrorDialogText = "Error communicating with server. Server reported error code ${
              t.response()?.code()
            }. Please try again later"
          )
        }
      }
    } else {
      _state.update {
        it.copy(
          loginProcessing = false,
          loginErrorDialogText = "Create Account failed. Debug info: '${t.message}'"
        )
      }
    }
  }

  fun onGoogleTokenReceived(token: String) {
    ogsRestService.loginWithGoogle(token)
      .doOnComplete { ogsWebSocketService.ensureSocketConnected() }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(Schedulers.io())
      .subscribe(this::onLoginSuccess, this::onPasswordLoginFailure)
      .addToDisposable(subscriptions)
  }
}

sealed class Page {
  enum class LoginMethod { GOOGLE, PASSWORD }

  data class OnboardingPage(
    val art: Int,
    val title: String,
    val description: String,
    val continueButtonText: String,
  ) : Page()

  data class MultipleChoicePage(
    val question: String,
    val answers: List<String>
  ) : Page()

  data class NotificationPermissionPage(
    val title: String,
    val description: String,
    val allowButtonText: String,
    val skipButtonText: String
  ) : Page()

  object LoginPage : Page()
}


data class OnboardingState(
  val currentPageIndex: Int = 0,
  val totalPages: Int,
  val finish: Boolean = false,
  val onboardingDone: Boolean = false,
  val requestNotificationPermission: Boolean = false,
  val currentPage: Page,
  val isExistingAccount: Boolean = false,
  val loginMethod: Page.LoginMethod? = null,
  val username: String = "",
  val password: String = "",
  val email: String = "",
  val logInButtonEnabled: Boolean = false,
  val loginProcessing: Boolean = false,
  val loginErrorDialogText: String? = null,
  val showOfflineConfirmationDialog: Boolean = false,
)

sealed class OnboardingAction {
  object ContinueClicked : OnboardingAction()
  object BackPressed : OnboardingAction()
  object LoginPressed : OnboardingAction()
  object DialogDismissed : OnboardingAction()
  object SocialPlatformLoginFailed : OnboardingAction()
  object AllowNotificationsClicked : OnboardingAction()
  object SkipNotificationsClicked : OnboardingAction()
  object PermissionsGranted : OnboardingAction()
  object ConfirmStayOffline : OnboardingAction()
  object CancelStayOffline : OnboardingAction()


  class AnswerSelected(val answerIndex: Int) : OnboardingAction()
  class UsernameChanged(val newUsername: String) : OnboardingAction()
  class EmailChanged(val newEmail: String) : OnboardingAction()
  class PasswordChanged(val newPassword: String) : OnboardingAction()
}