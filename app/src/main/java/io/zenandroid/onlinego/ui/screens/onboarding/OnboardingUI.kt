package io.zenandroid.onlinego.ui.screens.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.art_login_1
import io.zenandroid.onlinego.resources.art_onboarding
import io.zenandroid.onlinego.resources.cancel
import io.zenandroid.onlinego.resources.email
import io.zenandroid.onlinego.resources.ok
import io.zenandroid.onlinego.resources.onboarding_answer_google
import io.zenandroid.onlinego.resources.onboarding_answer_offline
import io.zenandroid.onlinego.resources.onboarding_answer_password
import io.zenandroid.onlinego.resources.onboarding_continue
import io.zenandroid.onlinego.resources.onboarding_create_account
import io.zenandroid.onlinego.resources.onboarding_google_signin_failed
import io.zenandroid.onlinego.resources.onboarding_link_account
import io.zenandroid.onlinego.resources.onboarding_login_failed_title
import io.zenandroid.onlinego.resources.onboarding_notifications_allow
import io.zenandroid.onlinego.resources.onboarding_notifications_description
import io.zenandroid.onlinego.resources.onboarding_notifications_skip
import io.zenandroid.onlinego.resources.onboarding_notifications_title
import io.zenandroid.onlinego.resources.onboarding_page_go_description
import io.zenandroid.onlinego.resources.onboarding_page_go_title
import io.zenandroid.onlinego.resources.onboarding_question_login_method
import io.zenandroid.onlinego.resources.onboarding_stay_offline_confirm
import io.zenandroid.onlinego.resources.onboarding_stay_offline_message
import io.zenandroid.onlinego.resources.onboarding_stay_offline_title
import io.zenandroid.onlinego.resources.password
import io.zenandroid.onlinego.resources.username
import io.zenandroid.onlinego.ui.composables.BackHandler
import io.zenandroid.onlinego.ui.composables.HorizontalPagerIndicator
import io.zenandroid.onlinego.ui.composables.resolve
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.BackPressed
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.SocialPlatformLoginFailed
import io.zenandroid.onlinego.ui.screens.onboarding.Page.OnboardingPage
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel = koinViewModel(),
  onNavigateToMyGames: () -> Unit,
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  val coroutineScope = rememberCoroutineScope()

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.onAction(OnboardingAction.PermissionsGranted)
  }

  val googleFlow = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      try {
        GoogleSignIn.getSignedInAccountFromIntent(result.data)
          .getResult(ApiException::class.java)?.serverAuthCode?.let {
            viewModel.onGoogleTokenReceived(it)
          }
      } catch (e: ApiException) {
        Logger.w("signInResult:failed code=" + e.statusCode, tag = "OnboardingFragment")
        CrashReporter.recordException(e)
        coroutineScope.launch {
          Toast.makeText(
            activity,
            getString(Res.string.onboarding_google_signin_failed, e.statusCode),
            Toast.LENGTH_LONG
          ).show()
        }
        viewModel.onAction(SocialPlatformLoginFailed)
      }
    } else {
      viewModel.onAction(SocialPlatformLoginFailed)
      Logger.e("Google sign-in cancelled", tag = "OnboardingScreen")
    }
  }

  BackHandler {
    viewModel.onAction(BackPressed)
  }

  if (state.requestNotificationPermission) {
    LaunchedEffect(Unit) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  when {
    state.finish -> onNavigateBack()
    state.onboardingDone -> onNavigateToMyGames()

    else -> {
      OnlineGoTheme {
        OnboardingContent(state, viewModel::onAction,
          onGoogleFlow = {
            CrashReporter.setCustomKey("LOGIN_METHOD", "GOOGLE")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestServerAuthCode("870935345166-6j2s6i9adl64ms3ta4k9n4flkqjhs229.apps.googleusercontent.com")
              .requestScopes(Scope(Scopes.OPEN_ID), Scope(Scopes.EMAIL), Scope(Scopes.PROFILE))
              .build()
            activity?.let {
              googleFlow.launch(GoogleSignIn.getClient(activity, gso).signInIntent)
            }
          }
        )
      }
    }
  }
}

@ExperimentalFoundationApi
@Composable
fun OnboardingContent(
  state: OnboardingState,
  listener: (OnboardingAction) -> Unit,
  onGoogleFlow: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .padding(36.dp, 16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      PageIndicator(
        currentPage = state.currentPageIndex,
        numberOfPages = state.totalPages,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
      when (state.currentPage) {
        is Page.OnboardingPage -> InfoPage(
          page = state.currentPage,
          listener = listener
        )

        is Page.MultipleChoicePage -> QuestionPage(
          page = state.currentPage,
          listener = listener
        )

        is Page.LoginPage -> when (state.loginMethod) {
          Page.LoginMethod.GOOGLE -> {
            SideEffect {
              onGoogleFlow()
            }
          }

          Page.LoginMethod.PASSWORD -> LoginPage(
            state = state,
            listener = listener
          )
          else -> {}
        }

        is Page.NotificationPermissionPage -> NotificationPermissionPage(
          page = state.currentPage,
          listener = listener
        )
      }
    }

    if (state.showOfflineConfirmationDialog) {
      OfflineConfirmationDialog(
        onConfirm = { listener(OnboardingAction.ConfirmStayOffline) },
        onDismiss = { listener(OnboardingAction.CancelStayOffline) }
      )
    }
  }
}

@Composable
private fun ColumnScope.LoginPage(
  state: OnboardingState,
  listener: (OnboardingAction) -> Unit
) {
  val focusManager = LocalFocusManager.current

  Column(
    modifier = Modifier
      .weight(1f)
      .verticalScroll(rememberScrollState())
      .imePadding()
  ) {
    Spacer(modifier = Modifier.weight(.5f))
    Surface(
      border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.primary),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Image(
        painter = painterResource(Res.drawable.art_login_1),
        colorFilter = ColorFilter.tint(
          MaterialTheme.colorScheme.primaryContainer,
          blendMode = BlendMode.Hue
        ),
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 32.dp)
      )
    }
    Spacer(modifier = Modifier.weight(.5f))

    if (!state.isExistingAccount) {
      OutlinedTextField(
        value = state.email,
        label = { Text(stringResource(Res.string.email)) },
        enabled = !state.loginProcessing,
        onValueChange = { listener.invoke(OnboardingAction.EmailChanged(it)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
          onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        modifier = Modifier
          .padding(vertical = 8.dp)
          .fillMaxWidth(),
      )
    }
    OutlinedTextField(
      value = state.username,
      label = { Text(stringResource(Res.string.username)) },
      enabled = !state.loginProcessing,
      onValueChange = { listener.invoke(OnboardingAction.UsernameChanged(it)) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
      ),
      keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) }
      ),
      modifier = Modifier
        .padding(vertical = 8.dp)
        .fillMaxWidth(),
    )

    var passwordVisibility by remember { mutableStateOf(false) }

    OutlinedTextField(
      value = state.password,
      label = { Text(stringResource(Res.string.password)) },
      enabled = !state.loginProcessing,
      onValueChange = { listener.invoke(OnboardingAction.PasswordChanged(it)) },
      visualTransformation = if (!passwordVisibility) PasswordVisualTransformation() else VisualTransformation.None,
      singleLine = true,
      trailingIcon = {
        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
          Icon(
            imageVector = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = ""
          )
        }
      },
      keyboardActions = KeyboardActions(
        onDone = { if (state.logInButtonEnabled) listener.invoke(OnboardingAction.LoginPressed) }
      ),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      modifier = Modifier
        .padding(vertical = 8.dp)
        .fillMaxWidth(),
    )
    Spacer(modifier = Modifier.weight(.5f))
  }

  Button(
    onClick = { listener.invoke(OnboardingAction.LoginPressed) },
    enabled = state.logInButtonEnabled && !state.loginProcessing,
    modifier = Modifier
      .padding(vertical = 8.dp)
      .fillMaxWidth()
  ) {
    if (!state.loginProcessing) {
      Text(
        text = if (state.isExistingAccount) stringResource(Res.string.onboarding_link_account)
        else stringResource(Res.string.onboarding_create_account),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    } else {
      CircularProgressIndicator()
    }
  }

  if (state.loginErrorDialogText != null) {
    AlertDialog(
      onDismissRequest = { listener(OnboardingAction.DialogDismissed) },
      confirmButton = {
        Button(onClick = { listener(OnboardingAction.DialogDismissed) }) {
          Text(text = stringResource(Res.string.ok))
        }
      },
      title = {
        Text(
          text = stringResource(Res.string.onboarding_login_failed_title),
          style = MaterialTheme.typography.titleLarge
        )
      },
      text = { Text(text = state.loginErrorDialogText.resolve()) }
    )
  }
}

@Composable
fun OfflineConfirmationDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        stringResource(Res.string.onboarding_stay_offline_title),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = { Text(stringResource(Res.string.onboarding_stay_offline_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.onboarding_stay_offline_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.cancel))
      }
    }
  )
}
@Composable
private fun ColumnScope.QuestionPage(
  page: Page.MultipleChoicePage,
  listener: (OnboardingAction) -> Unit
) {
  Spacer(modifier = Modifier.weight(.5f))
  Text(
    text = stringResource(page.question),
    style = MaterialTheme.typography.headlineLarge,
    textAlign = TextAlign.Center,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )
  Spacer(modifier = Modifier.weight(.5f))
  page.answers.forEachIndexed { index, answer ->
    val isThirdButton = index == 2
    if (!isThirdButton) {
      Button(
        onClick = { listener.invoke(OnboardingAction.AnswerSelected(index)) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Text(text = stringResource(answer))
      }
    } else {
      TextButton(
        onClick = { listener.invoke(OnboardingAction.AnswerSelected(index)) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Text(text = stringResource(answer))
      }
    }
  }
  Spacer(modifier = Modifier.weight(2f))
}

@Composable
private fun ColumnScope.NotificationPermissionPage(
  page: Page.NotificationPermissionPage,
  listener: (OnboardingAction) -> Unit
) {
  Spacer(modifier = Modifier.weight(.25f))

  Image(
    Icons.Rounded.Notifications,
    contentDescription = null,
    colorFilter = ColorFilter.tint(LocalContentColor.current),
    modifier = Modifier
      .size(144.dp)
      .align(Alignment.CenterHorizontally)
  )
  Spacer(modifier = Modifier.weight(.25f))

  Text(
    text = stringResource(page.title),
    style = MaterialTheme.typography.headlineLarge,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )

  Text(
    text = stringResource(page.description),
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.bodyMedium,
    lineHeight = 20.sp,
    modifier = Modifier
      .weight(.5f)
      .wrapContentHeight(Alignment.CenterVertically)
      .align(Alignment.CenterHorizontally)
      .padding(horizontal = 16.dp)
  )

  Button(
    onClick = { listener(OnboardingAction.AllowNotificationsClicked) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Text(
      text = stringResource(page.allowButtonText),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
  }

  Button(
    onClick = { listener(OnboardingAction.SkipNotificationsClicked) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Text(text = stringResource(page.skipButtonText), fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}


@Composable
private fun ColumnScope.InfoPage(page: Page.OnboardingPage, listener: (OnboardingAction) -> Unit) {
  Image(
    painter = painterResource(page.art),
    contentDescription = null,
    modifier = Modifier
      .padding(vertical = 70.dp)
      .weight(.5f)
      .align(Alignment.CenterHorizontally)
      .fillMaxWidth()
  )
  Text(
    text = stringResource(page.title),
    style = MaterialTheme.typography.headlineLarge,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )
  Text(
    text = stringResource(page.description),
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.bodyMedium,
    lineHeight = 20.sp,
    modifier = Modifier
      .weight(.5f)
      .wrapContentHeight(Alignment.CenterVertically)
      .align(Alignment.CenterHorizontally)
  )
  Button(
    onClick = { listener(OnboardingAction.ContinueClicked) },
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = stringResource(page.continueButtonText),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@ExperimentalFoundationApi
@Composable
private fun PageIndicator(modifier: Modifier = Modifier, currentPage: Int, numberOfPages: Int) {
  val state = rememberPagerState { numberOfPages }
  HorizontalPager(state = state) {} // Dirty hack to set the number of pages since that's no longer supported

  LaunchedEffect(currentPage) {
    state.animateScrollToPage(currentPage)
  }
  HorizontalPagerIndicator(
    pagerState = state,
    pageCount = state.pageCount,
    activeColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier
      .padding(16.dp)
      .clickable(enabled = false) {}
  )
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        totalPages = 6,
        currentPage = OnboardingPage(
          Res.drawable.art_onboarding,
          Res.string.onboarding_page_go_title,
          Res.string.onboarding_page_go_description,
          Res.string.onboarding_continue
        ),
      ),
      {},
      {},
    )
  }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview1() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        currentPage = Page.MultipleChoicePage(
          Res.string.onboarding_question_login_method,
          listOf(
            Res.string.onboarding_answer_google,
            Res.string.onboarding_answer_password,
            Res.string.onboarding_answer_offline
          )
        ),
        totalPages = 6,
      ),
      {},
      {},
      )
  }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview2() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        currentPage = Page.LoginPage,
        loginMethod = Page.LoginMethod.PASSWORD,
        isExistingAccount = false,
        totalPages = 6,
      ),
      {},
      {},
      )
  }
}

@Preview
@Composable
fun DefaultPreview3() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        currentPage = Page.NotificationPermissionPage(
          title = Res.string.onboarding_notifications_title,
          description = Res.string.onboarding_notifications_description,
          allowButtonText = Res.string.onboarding_notifications_allow,
          skipButtonText = Res.string.onboarding_notifications_skip
        ),
        totalPages = 6,
      ),
      {},
      {},
      )
  }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview4() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        currentPage = Page.MultipleChoicePage(
          Res.string.onboarding_question_login_method,
          listOf(
            Res.string.onboarding_answer_google,
            Res.string.onboarding_answer_password,
            Res.string.onboarding_answer_offline
          )
        ),
        totalPages = 6,
        showOfflineConfirmationDialog = true,
      ),
      {},
      {},
      )
  }
}
