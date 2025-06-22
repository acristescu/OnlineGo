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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
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
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.login.FacebookLoginCallbackActivity
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.BackPressed
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingAction.SocialPlatformLoginFailed
import io.zenandroid.onlinego.ui.screens.onboarding.Page.LoginMethod
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
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
            recordException(e)
            Toast.makeText(requireContext(), "signInResult:failed code=" + e.statusCode, Toast.LENGTH_LONG).show()
            viewModel.onAction(SocialPlatformLoginFailed)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return ComposeView(requireContext()).apply {
            setContent {
                val state by rememberStateWithLifecycle(viewModel.state)

                when {
                    state.finish -> {
                        requireActivity().finish()
                    }
                    state.loginSuccessful -> {
//                        findNavController().navigate(R.id.onboarding_to_mygames)
                        (requireActivity() as MainActivity).askForNotificationsPermission(true)
                    }
//                    state.loginMethod == LoginMethod.FACEBOOK -> doFacebookFlow()
                    state.loginMethod == LoginMethod.GOOGLE -> doGoogleFlow()
                    else -> {
                        OnlineGoTheme {
                            Screen(state, viewModel::onAction)
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

    /**
     * This stopped working in Android 12 because:
     * Starting in Android 12 (API level 31), a generic web intent resolves to an activity in your
     * app only if your app is approved for the specific domain contained in that web intent. If
     * your app isn't approved for the domain, the web intent resolves to the user's default
     * browser app instead.
     */
    private fun doFacebookFlow() {
        FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "FACEBOOK")
        val url = BuildConfig.BASE_URL + "/login/facebook/"
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
                recordException(it)
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                viewModel.onAction(SocialPlatformLoginFailed)
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