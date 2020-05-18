package io.zenandroid.onlinego.ui.screens.login

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.transition.Fade
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.hide
import io.zenandroid.onlinego.utils.onChange
import io.zenandroid.onlinego.utils.show
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.HttpException


/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    private var createAccount = false

    private var analytics = OnlineGoApplication.instance.analytics
    private val subscriptions = CompositeDisposable()
    private val client = OkHttpClient.Builder()
            .cookieJar(OGSServiceImpl.cookieJar)
            .followRedirects(false)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_login)

        noAccountView.setOnClickListener { toggleCreateAccountMode() }
        loginButton.setOnClickListener { onLoginClicked() }

        username.onChange { onTextChanged() }
        password.onChange { onTextChanged() }

        intent.data?.let {
            val request = Request.Builder()
                    .url(it.toString())
                    .get()
                    .build()
            Completable.fromCallable { client.newCall(request).execute() }
                    .andThen(OGSServiceImpl.fetchUIConfig())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onLoginSuccess, this::onTokenLoginFailure)
        }
        facebookSignInButton.setOnClickListener {
            Crashlytics.setString("LOGIN_METHOD", "FACEBOOK")
            socialPlatformLogin("https://online-go.com/login/facebook/")
        }
        googleSignInButton.setOnClickListener {
            Crashlytics.setString("LOGIN_METHOD", "GOOGLE")
            socialPlatformLogin("https://online-go.com/login/google-oauth2/")
        }
    }

    private fun socialPlatformLogin(url: String) {
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        Single.fromCallable { client.newCall(request).execute() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ response ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.header("Location"))))
                } , {
                    Crashlytics.logException(it)
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }).addToDisposable(subscriptions)
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

        if(OGSServiceImpl.isLoggedIn()) {
            OGSServiceImpl.ensureSocketConnected()
            onLoginSuccess()
        } else if(intent.data == null) {
            onTokenLoginFailure(java.lang.Exception())
        }
    }

    private fun onLoginSuccess() {
        Crashlytics.setString("LOGIN_METHOD", "PASSWORD")
        if(loginButton.visibility == View.VISIBLE) {
            val drawable = ContextCompat.getDrawable (this, R.drawable.ic_done_24dp)

            val bitmap = Bitmap.createBitmap (loginButton.width, loginButton.height, Bitmap.Config.ARGB_8888);
            val canvas = Canvas(bitmap)
            drawable?.setBounds(0, 0, canvas.width, canvas.height)
            drawable?.draw(canvas)
            loginButton.doneLoadingAnimation(ResourcesCompat.getColor(resources, R.color.colorAccent, null), bitmap)
        }
        TransitionManager.beginDelayedTransition(findViewById(R.id.container), Fade(Fade.MODE_OUT).setDuration(100).setStartDelay(400))
        findViewById<ViewGroup>(R.id.container).removeAllViews()
        Handler().postDelayed({
            val newIntent = Intent(this, MainActivity::class.java).apply {
                if(intent.hasExtra("GAME_ID")) {
                    putExtra("GAME_ID", intent.getLongExtra("GAME_ID", 0))
                }
            }
            startActivity(newIntent)
            finish()
        }, 500)
    }

    private fun onTokenLoginFailure(t: Throwable) {
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
        if( (t as? HttpException)?.code() in arrayOf(401, 403) ) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Login failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    private fun onLoginClicked() {
        loginButton.startAnimation()
        if(!createAccount) {
            doLogin()
        } else {
            OGSServiceImpl.createAccount(username.text.toString().trim(), password.text.toString(), email.text.toString().trim())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::onCreateAccountSuccess,
                            this::onCreateAccountFailure
                    )
                    .addToDisposable(subscriptions)
        }
    }

    private fun doLogin() {
        OGSServiceImpl.login(username.text.toString(), password.text.toString())
                .doOnComplete { OGSServiceImpl.ensureSocketConnected() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null) }
                .subscribe(
                        this::onLoginSuccess,
                        this::onPasswordLoginFailure
                )
                .addToDisposable(subscriptions)
    }

    private fun onCreateAccountSuccess() {
        Crashlytics.setBool("NEW_ACCOUNT", true)
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