package io.zenandroid.onlinego.ui.screens.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.login.FacebookLoginCallbackActivity
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.*
import io.zenandroid.onlinego.ui.screens.onboarding.Page.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.addToDisposable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingFragment : Fragment() {
    private val viewModel: OnboardingViewModel by viewModel()

    private val userSessionRepository: UserSessionRepository by inject()
    private val client = OkHttpClient.Builder()
        .cookieJar(userSessionRepository.cookieJar)
        .followRedirects(false)
        .build()
    private val subscriptions = CompositeDisposable()
    private val googleFlow = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        try {
            GoogleSignIn.getSignedInAccountFromIntent(it.data).getResult(ApiException::class.java)?.serverAuthCode?.let {
                viewModel.onGoogleTokenReceived(it)
            }
        } catch (e: ApiException) {
            Log.w("OnboardingFragment", "signInResult:failed code=" + e.statusCode)
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(requireContext(), "signInResult:failed code=" + e.statusCode, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.observeAsState()

                state?.let {
                    when {
                        state?.finish == true -> {
                            requireActivity().finish()
                        }
                        state?.loginSuccessful == true -> {
                            findNavController().navigate(R.id.onboarding_to_mygames)
                        }
                        state?.loginMethod == LoginMethod.FACEBOOK -> doFacebookFlow()
                        state?.loginMethod == LoginMethod.GOOGLE -> doGoogleFlow()
                        else -> {
                            OnlineGoTheme {
                                Screen(it, viewModel::onAction)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onAction(BackPressed)
            }
        })
    }

    private fun doFacebookFlow() {
        FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "FACEBOOK")
        val url = "https://online-go.com/login/facebook/"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        Single.fromCallable { client.newCall(request).execute() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                requireActivity().packageManager.setComponentEnabledSetting(
                    ComponentName(requireContext(), FacebookLoginCallbackActivity::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.header("Location"))))
            }, {
                FirebaseCrashlytics.getInstance().recordException(it)
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }).addToDisposable(subscriptions)
    }

    private fun doGoogleFlow() {
        FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "GOOGLE")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode("870935345166-6j2s6i9adl64ms3ta4k9n4flkqjhs229.apps.googleusercontent.com")
            .requestScopes(Scope(Scopes.OPEN_ID), Scope(Scopes.EMAIL), Scope(Scopes.PROFILE))
            .build()
        googleFlow.launch(GoogleSignIn.getClient(requireActivity(), gso).signInIntent)
    }

    override fun onPause() {
        super.onPause()
        subscriptions.clear()
    }
}

@Composable
fun Screen(state: OnboardingState, listener: (OnboardingAction) -> Unit) {
    Column (modifier = Modifier
        .fillMaxHeight()
        .background(Color.White)
        .padding(36.dp, 16.dp)
    ) {
        PageIndicator(
            currentPage = state.currentPageIndex,
            numberOfPages = state.totalPages,
            modifier = Modifier.align(CenterHorizontally)
        )
        when (state.currentPage) {
            is OnboardingPage -> InfoPage(page = state.currentPage, listener = listener)
            is MultipleChoicePage -> QuestionPage(page = state.currentPage, listener = listener)
            is LoginPage -> when (state.loginMethod!!) {
                LoginMethod.GOOGLE -> {}
                LoginMethod.FACEBOOK -> {}
                LoginMethod.PASSWORD -> LoginPage(
                    state = state,
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

    Column(modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
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
                onValueChange = { listener.invoke(EmailChanged(it)) },
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
            onValueChange = { listener.invoke(UsernameChanged(it)) },
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
            onValueChange = { listener.invoke(PasswordChanged(it)) },
            visualTransformation = if(!passwordVisibility) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                    Icon(
                        imageVector  = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = ""
                    )
                }
                           },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.weight(.5f))
    }

    Button(
        onClick = { listener.invoke(LoginPressed) },
        enabled = state.logInButtonEnabled && !state.loginProcessing,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        if(!state.loginProcessing) {
            Text(
                text = if (state.isExistingAccount) "Link OGS account" else "Create OGS account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            CircularProgressIndicator()
        }
    }

    if(state.loginErrorDialogText != null) {
        AlertDialog(
            onDismissRequest = { listener.invoke(DialogDismissed) },
            confirmButton = {
                Button(onClick = { listener.invoke(DialogDismissed) }) {
                    Text(text = "OK")
                }
            },
            title = { Text(text = "Log in failed")},
            text = { Text(text = state.loginErrorDialogText)}
        )
    }
}

@Composable
private fun ColumnScope.QuestionPage(page: MultipleChoicePage, listener: (OnboardingAction) -> Unit) {
    Spacer(modifier = Modifier.weight(.5f))
    Text(
        text = page.question,
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(CenterHorizontally)
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
private fun ColumnScope.InfoPage(page: OnboardingPage, listener: (OnboardingAction) -> Unit) {
    Image(
        painter = painterResource(id = page.art),
        contentDescription = null,
        modifier = Modifier
            .padding(vertical = 70.dp)
            .weight(.5f)
            .align(CenterHorizontally)
            .fillMaxWidth()
    )
    Text(
        text = page.title,
        style = MaterialTheme.typography.h6,
        modifier = Modifier.align(CenterHorizontally)
    )
    Text(
        text = page.description,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body1,
        lineHeight = 20.sp,
        modifier = Modifier
            .weight(.5f)
            .wrapContentHeight(CenterVertically)
            .align(CenterHorizontally)
    )
        Button(onClick = { listener(OnboardingAction.ContinueClicked) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = page.continueButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
}

@Composable
private fun PageIndicator(modifier: Modifier = Modifier, currentPage: Int, numberOfPages: Int) {
    Row(modifier = modifier) {
        repeat(times = numberOfPages) { page ->
            val circleColor = if (page == currentPage) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp) // this is the space between the dots
                    .background(Color.Black, shape = CircleShape)
                    .padding(all = 1.dp) // width of the line of the empty circle
                    .background(color = circleColor, shape = CircleShape)
                    .size(6.dp) // size of the middle circle
            )
        }
    }
}

@Preview
@Composable
fun DefaultPreview() {
    OnlineGoTheme {
        Screen(OnboardingState()) { _ -> }
    }
}

@Preview
@Composable
fun DefaultPreview1() {
    OnlineGoTheme {
        Screen(OnboardingState(currentPage = MultipleChoicePage("Is there a cow level? Lorem ipsum dolor sit amet", listOf("Yes", "NO!!!")))) { _ -> }
    }
}

@Preview
@Composable
fun DefaultPreview2() {
    OnlineGoTheme {
        Screen(OnboardingState(currentPage = LoginPage, loginMethod = LoginMethod.PASSWORD, isExistingAccount = false)) { _ -> }
    }
}