package io.zenandroid.onlinego.ui.screens.onboarding

import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
      R.string.onboarding_page_go_title,
      R.string.onboarding_page_go_description,
      R.string.onboarding_continue
    ),
    OnboardingPage(
      R.drawable.ic_board_transparent,
      R.string.onboarding_page_app_title,
      R.string.onboarding_page_app_description,
      R.string.onboarding_continue
    ),
    OnboardingPage(
      R.drawable.logo_ogs,
      R.string.onboarding_page_ogs_title,
      R.string.onboarding_page_ogs_description,
      R.string.onboarding_link_account
    ),
    MultipleChoicePage(
      R.string.onboarding_question_has_account,
      listOf(R.string.yes, R.string.no)
    ),
    MultipleChoicePage(
      R.string.onboarding_question_login_method,
      listOf(
        R.string.onboarding_answer_google,
        R.string.onboarding_answer_password,
        R.string.onboarding_answer_offline
      )
    ),
    LoginPage,
    NotificationPermissionPage(
      R.string.onboarding_notifications_title,
      R.string.onboarding_notifications_description,
      R.string.onboarding_notifications_allow,
      R.string.onboarding_notifications_skip
    )
  ).drop(if (savedStateHandle["initialPageArg"] as String? != null) 4 else 0)

  private val analytics = OnlineGoApplication.instance.analytics

  init {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val loggedIn = userSessionRepository.loginStatus.first()
        if (loggedIn is LoginStatus.LoggedIn) {
          //
          // This should only happen during the migration
          //
          settingsRepository.setHasCompletedOnboarding(true)
          _state.update { it.copy(onboardingDone = true) }
        }
      } catch (e: Exception) {
        recordException(e)
      }
    }
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


  fun onAction(action: OnboardingAction) {
    when (action) {
      OnboardingAction.BackPressed -> {
        if(state.value.currentPage is Page.NotificationPermissionPage) {
          onAction(OnboardingAction.SkipNotificationsClicked)
        } else if (state.value.currentPageIndex != 0) {
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
      viewModelScope.launch(Dispatchers.IO) {
        try {
          ogsRestService.createAccount(state.username.trim(), state.password, state.email.trim())
          FirebaseCrashlytics.getInstance().setCustomKey("NEW_ACCOUNT", true)
          analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
          doLogin(_state.value)
        } catch (t: Throwable) {
          onCreateAccountFailure(t)
        }
      }
    }
  }

  private fun doLogin(state: OnboardingState) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        ogsRestService.login(state.username.trim(), state.password)
        ogsWebSocketService.ensureSocketConnected()
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
        onLoginSuccess()
      } catch (t: Throwable) {
        onPasswordLoginFailure(t)
      }
    }
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
    _state.update {
      it.copy(loginMethod = Page.LoginMethod.GOOGLE)
    }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        ogsRestService.loginWithGoogle(token)
        ogsWebSocketService.ensureSocketConnected()
        withContext(Dispatchers.Main) { onLoginSuccess() }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { onPasswordLoginFailure(e) }
      }
    }
  }
}

sealed class Page {
  enum class LoginMethod { GOOGLE, PASSWORD }

  data class OnboardingPage(
    @DrawableRes val art: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val continueButtonText: Int,
  ) : Page()

  data class MultipleChoicePage(
    @StringRes val question: Int,
    val answers: List<Int>
  ) : Page()

  data class NotificationPermissionPage(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val allowButtonText: Int,
    @StringRes val skipButtonText: Int
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