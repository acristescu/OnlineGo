package io.zenandroid.onlinego.ui.screens.login

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.transition.Fade
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.*
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.android.ext.android.inject
import retrofit2.HttpException


/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    private val userSessionRepository: UserSessionRepository by inject()
    private val ogsRestService: OGSRestService by inject()
    private val ogsSocketService: OGSWebSocketService by inject()
    private val idlingResource: CountingIdlingResource by inject()

    private var createAccount = false

    private var analytics = OnlineGoApplication.instance.analytics
    private val subscriptions = CompositeDisposable()
    private val client = OkHttpClient.Builder()
            .cookieJar(userSessionRepository.cookieJar)
            .followRedirects(false)
            .build()
    private val callbackManager = CallbackManager.Factory.create()


    private var googleLoginInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageManager.setComponentEnabledSetting(
                ComponentName(this, FacebookLoginCallbackActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        )

        supportActionBar?.hide()

        setContentView(R.layout.activity_login)

        noAccountView.setOnClickListener { toggleCreateAccountMode() }
        loginButton.setOnClickListener { onLoginClicked() }

        username.onChange { onTextChanged() }
        password.onChange { onTextChanged() }

        login_button.setPermissions(listOf("email"))
        login_button.authType = "code"
        login_button.registerCallback(callbackManager, object: FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                Log.e("*****", result?.toString())
                Log.e("*****", result!!.accessToken.token)
            }

            override fun onCancel() {
                Log.e("*****", "cancel")
            }

            override fun onError(error: FacebookException?) {
                Log.e("*****", error?.toString())
            }

        })

        intent.data?.let {
            val request = Request.Builder()
                    .url(it.toString())
                    .get()
                    .build()
            Completable.fromCallable { client.newCall(request).execute() }
                    .andThen(ogsRestService.fetchUIConfig())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onLoginSuccess, this::onTokenLoginFailure)
        }
        facebookSignInButton.setOnClickListener { doFacebookFlow() }
        googleSignInButton.setOnClickListener { doGoogleFlow() }
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
                    packageManager.setComponentEnabledSetting(
                            ComponentName(this, FacebookLoginCallbackActivity::class.java),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                    )

                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.header("Location"))))
                }, {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }).addToDisposable(subscriptions)
    }

    private fun doGoogleFlow() {
        FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "GOOGLE")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode("870935345166-6j2s6i9adl64ms3ta4k9n4flkqjhs229.apps.googleusercontent.com")
                .requestScopes(Scope(Scopes.OPEN_ID), Scope(Scopes.EMAIL), Scope(Scopes.PROFILE))
                .build()
        startActivityForResult(GoogleSignIn.getClient(this, gso).signInIntent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                account?.serverAuthCode?.let {

                    googleLoginInProgress = true
                    loginButton.visibility = View.GONE
                    usernameLayout.visibility = View.GONE
                    passwordLayout.visibility = View.GONE
                    noAccountView.visibility = View.GONE
                    facebookSignInButton.visibility = View.GONE
                    googleSignInButton.visibility = View.GONE

                    ogsRestService.loginWithGoogle(it)
                            .doOnComplete { ogsSocketService.ensureSocketConnected() }
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(this::proceedToMainScreen, this::onPasswordLoginFailure)
                            .addToDisposable(subscriptions)
                }

            } catch (e: ApiException) {
                Log.w(TAG, "signInResult:failed code=" + e.statusCode)
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "signInResult:failed code=" + e.statusCode, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleCreateAccountMode() {
        TransitionManager.beginDelayedTransition(findViewById(R.id.container))
        createAccount = !createAccount
        if(createAccount) {
            emailLayout.show()
            noAccountView.setText(R.string.signing_prompt)
            loginButton.setText(R.string.create_account)
        } else {
            emailLayout.hide()
            noAccountView.setText(R.string.create_account_prompt)
            loginButton.setText(R.string.login_to_ogs)
        }
    }

    override fun onResume() {
        super.onResume()

        if(userSessionRepository.isLoggedIn()) {
            ogsSocketService.ensureSocketConnected()
            onLoginSuccess()
        } else if(intent.data == null && !googleLoginInProgress) {
            onTokenLoginFailure(java.lang.Exception())
        }
    }

    private fun onLoginSuccess() {
        if(loginButton.visibility == View.VISIBLE) {
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_done_24dp)

            val bitmap = Bitmap.createBitmap(loginButton.width, loginButton.height, Bitmap.Config.ARGB_8888);
            val canvas = Canvas(bitmap)
            drawable?.setBounds(0, 0, canvas.width, canvas.height)
            drawable?.draw(canvas)
            loginButton.doneLoadingAnimation(ResourcesCompat.getColor(resources, R.color.colorAccent, null), bitmap)
        }
        idlingResource.increment()
        TransitionManager.beginDelayedTransition(findViewById(R.id.container), Fade(Fade.MODE_OUT).setDuration(100).setStartDelay(400))
        findViewById<ViewGroup>(R.id.container).removeAllViews()
        Handler().postDelayed({
            proceedToMainScreen()
            idlingResource.decrement()
        }, 500)
    }

    private fun proceedToMainScreen() {
        val newIntent = Intent(this, MainActivity::class.java).apply {
            if (intent.hasExtra("GAME_ID")) {
                putExtra("GAME_ID", intent.getLongExtra("GAME_ID", 0))
            }
        }
        startActivity(newIntent)
        finish()
    }

    private fun onTokenLoginFailure(t: Throwable) {
        googleLoginInProgress = false
        TransitionManager.beginDelayedTransition(
                findViewById(R.id.container),
                TransitionInflater.from(this).inflateTransition(R.transition.login)
        )
        val params = logo.layoutParams as LinearLayout.LayoutParams
        params.weight = 0f
        loginButton.visibility = View.VISIBLE
        usernameLayout.visibility = View.VISIBLE
        passwordLayout.visibility = View.VISIBLE
        noAccountView.visibility = View.VISIBLE
        facebookSignInButton.visibility = View.VISIBLE
        googleSignInButton.visibility = View.VISIBLE
    }

    private fun onPasswordLoginFailure(t: Throwable) {
        loginButton.revertAnimation()
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        FirebaseCrashlytics.getInstance().recordException(t)
        if( (t as? HttpException)?.code() in arrayOf(401, 403) ) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Login failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    private fun onLoginClicked() {
        FirebaseCrashlytics.getInstance().setCustomKey("LOGIN_METHOD", "PASSWORD")
        loginButton.startAnimation()
        if(!createAccount) {
            doLogin()
        } else {
            ogsRestService.createAccount(username.text.toString().trim(), password.text.toString(), email.text.toString().trim())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::onCreateAccountSuccess,
                            this::onCreateAccountFailure
                    )
                    .addToDisposable(subscriptions)
        }
    }

    private fun doLogin() {
        ogsRestService.login(username.text.toString(), password.text.toString())
                .doOnComplete { ogsSocketService.ensureSocketConnected() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null) }
                .subscribe(this::onLoginSuccess, this::onPasswordLoginFailure)
                .addToDisposable(subscriptions)
    }

    private fun onCreateAccountSuccess() {
        FirebaseCrashlytics.getInstance().setCustomKey("NEW_ACCOUNT", true)
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
        doLogin()
    }

    private fun onCreateAccountFailure(t: Throwable) {
        loginButton.revertAnimation {
            loginButton.setText(R.string.create_account)
        }
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        if(t is HttpException && t.response()?.errorBody() != null) {
            try {
                val error = JSONObject(t.response()?.errorBody()?.string())["error"].toString()
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Can't parse error: ${t.response()?.errorBody()?.string()}")
                Toast.makeText(this, "Error communicating with server. Server reported error code ${t.response()?.code()}. Please try again later", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Create Account failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    private fun onTextChanged() {
        loginButton.isEnabled = username.text?.isNotEmpty() == true && password.text?.isNotEmpty() == true
    }

    override fun onPause() {
        super.onPause()
        subscriptions.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginButton.dispose()
    }
}