package io.zenandroid.onlinego.ui.screens.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.BackPressed
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.SocialPlatformLoginFailed
import io.zenandroid.onlinego.ui.screens.onboarding.Page.LoginMethod
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel = koinViewModel(),
  onNavigateToMyGames: () -> Unit = {}
) {
  val state by rememberStateWithLifecycle(viewModel.state)
  val activity = LocalActivity.current

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    onNavigateToMyGames()
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
        Log.w("OnboardingFragment", "signInResult:failed code=" + e.statusCode)
        recordException(e)
        Toast.makeText(activity, "signInResult:failed code=" + e.statusCode, Toast.LENGTH_LONG)
          .show()
        viewModel.onAction(SocialPlatformLoginFailed)
      }
    } else {
      Log.e("OnboardingScreen", "Google sign-in cancelled")
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
    state.finish -> {
      LocalActivity.current?.finish()
    }

    state.loginMethod == LoginMethod.GOOGLE -> {
      FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "GOOGLE")
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode("870935345166-6j2s6i9adl64ms3ta4k9n4flkqjhs229.apps.googleusercontent.com")
        .requestScopes(Scope(Scopes.OPEN_ID), Scope(Scopes.EMAIL), Scope(Scopes.PROFILE))
        .build()
      activity?.let {
        googleFlow.launch(GoogleSignIn.getClient(activity, gso).signInIntent)
      }
    }

    else -> {
      OnlineGoTheme {
        OnboardingContent(state, viewModel::onAction)
      }
    }
  }
}

@ExperimentalFoundationApi
@Composable
fun OnboardingContent(state: OnboardingState, listener: (OnboardingAction) -> Unit) {
  Surface(modifier = Modifier.fillMaxSize()) {
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

        is Page.LoginPage -> when (state.loginMethod!!) {
          Page.LoginMethod.GOOGLE -> {
          }

          Page.LoginMethod.PASSWORD -> LoginPage(
            state = state,
            listener = listener
          )
        }

        is Page.NotificationPermissionPage -> NotificationPermissionPage(
          page = state.currentPage,
          listener = listener
        )
      }
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
    Image(
      painter = painterResource(id = R.drawable.art_login_1),
      contentDescription = null,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.weight(.5f))

    if (!state.isExistingAccount) {
      OutlinedTextField(
        value = state.email,
        label = { Text("Email") },
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
      label = { Text("Username") },
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
      label = { Text("Password") },
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
        text = if (state.isExistingAccount) "Link OGS account" else "Create OGS account",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    } else {
      CircularProgressIndicator()
    }
  }

  if (state.loginErrorDialogText != null) {
    AlertDialog(
      onDismissRequest = { listener.invoke(OnboardingAction.DialogDismissed) },
      confirmButton = {
        Button(onClick = { listener.invoke(OnboardingAction.DialogDismissed) }) {
          Text(text = "OK")
        }
      },
      title = { Text(text = "Log in failed") },
      text = { Text(text = state.loginErrorDialogText) }
    )
  }
}

@Composable
private fun ColumnScope.QuestionPage(
  page: Page.MultipleChoicePage,
  listener: (OnboardingAction) -> Unit
) {
  Spacer(modifier = Modifier.weight(.5f))
  Text(
    text = page.question,
    style = MaterialTheme.typography.headlineLarge,
    textAlign = TextAlign.Center,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )
  Spacer(modifier = Modifier.weight(.5f))
  for (answer in page.answers) {
    Button(
      onClick = { listener.invoke(OnboardingAction.AnswerSelected(page.answers.indexOf(answer))) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    ) {
      Text(text = answer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    text = page.title,
    style = MaterialTheme.typography.headlineLarge,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )

  Text(
    text = page.description,
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
    Text(text = page.allowButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }

  Button(
    onClick = { listener(OnboardingAction.SkipNotificationsClicked) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Text(text = page.skipButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}


@Composable
private fun ColumnScope.InfoPage(page: Page.OnboardingPage, listener: (OnboardingAction) -> Unit) {
  Image(
    painter = painterResource(id = page.art),
    contentDescription = null,
    modifier = Modifier
      .padding(vertical = 70.dp)
      .weight(.5f)
      .align(Alignment.CenterHorizontally)
      .fillMaxWidth()
  )
  Text(
    text = page.title,
    style = MaterialTheme.typography.headlineLarge,
    modifier = Modifier.align(Alignment.CenterHorizontally)
  )
  Text(
    text = page.description,
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
    Text(text = page.continueButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    OnboardingContent(OnboardingState()) { }
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
          "Is there a cow level? Lorem ipsum dolor sit amet",
          listOf("Yes", "NO!!!")
        )
      )
    ) { }
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
        isExistingAccount = false
      )
    ) { }
  }
}

@Preview
@Composable
fun DefaultPreview3() {
  OnlineGoTheme(darkTheme = true) {
    OnboardingContent(
      OnboardingState(
        currentPage = Page.NotificationPermissionPage(
          title = "Enable notifications",
          description = "Get notified about your games, messages and more.",
          allowButtonText = "Allow",
          skipButtonText = "Skip"
        )
      )
    ) { }
  }
}
