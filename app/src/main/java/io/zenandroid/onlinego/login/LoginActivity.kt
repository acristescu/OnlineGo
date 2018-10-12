package io.zenandroid.onlinego.login

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.support.transition.Fade
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.hide
import io.zenandroid.onlinego.extensions.show
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.onChange
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import retrofit2.HttpException


/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    private var createAccount = false

    private var analytics = OnlineGoApplication.instance.analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_login)

        noAccountView.setOnClickListener { toggleCreateAccountMode() }
        loginButton.setOnClickListener { onLoginClicked() }

//        val watcher = object : TextWatcher {
//            override fun afterTextChanged(p0: Editable?) {}
//
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                onTextChanged()
//            }
//        }
//        username.addTextChangedListener(watcher)
//        password.addTextChangedListener(watcher)

        username.onChange { onTextChanged() }
        password.onChange { onTextChanged() }
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

        OGSServiceImpl.instance.loginWithToken()
                .doOnComplete { OGSServiceImpl.instance.ensureSocketConnected() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoginSuccess, this::onTokenLoginFailure)
    }

    private fun onLoginSuccess() {
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
            startActivity(Intent(this, MainActivity::class.java))
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
    }

    private fun onPasswordLoginFailure(t: Throwable) {
        loginButton.revertAnimation()
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        if(t is HttpException && t.code() == 401) {
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
            OGSServiceImpl.instance.createAccount(username.text.toString().trim(), password.text.toString(), email.text.toString().trim())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::onCreateAccountSuccess,
                            this::onCreateAccountFailure
                    )
        }
    }

    private fun doLogin() {
        OGSServiceImpl.instance.login(username.text.toString(), password.text.toString())
                .doOnComplete { OGSServiceImpl.instance.ensureSocketConnected() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null) }
                .subscribe(
                        this::onLoginSuccess,
                        this::onPasswordLoginFailure
                )
    }

    private fun onCreateAccountSuccess() {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
        doLogin()
    }

    private fun onCreateAccountFailure(t: Throwable) {
        loginButton.revertAnimation {
            loginButton.setText(R.string.create_account)
        }
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        if(t is HttpException && t.response().errorBody() != null) {
            try {
                val error = JSONObject(t.response().errorBody()?.string())["error"].toString()
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Can't parse error: ${t.response().errorBody()?.string()}")
                Toast.makeText(this, "Error communicating with server. Server reported error code ${t.response().code()}. Please try again later", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Create Account failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    fun onTextChanged() {
        loginButton.isEnabled = username.text.isNotEmpty() && password.text.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginButton.dispose()
    }
}