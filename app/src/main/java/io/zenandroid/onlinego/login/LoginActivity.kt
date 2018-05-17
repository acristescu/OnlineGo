package io.zenandroid.onlinego.login

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.transition.Fade
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnTextChanged
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.socket.client.On.on
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.hide
import io.zenandroid.onlinego.extensions.show
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import org.json.JSONObject
import retrofit2.HttpException


/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    @BindView(R.id.input_username) lateinit var username: TextInputEditText
    @BindView(R.id.input_password) lateinit var password: TextInputEditText
    @BindView(R.id.input_email) lateinit var email: TextInputEditText
    @BindView(R.id.input_username_layout) lateinit var usernameLayout: TextInputLayout
    @BindView(R.id.input_password_layout) lateinit var passwordLayout: TextInputLayout
    @BindView(R.id.input_email_layout) lateinit var emailLayout: TextInputLayout
    @BindView(R.id.logo) lateinit var logo: AppCompatImageView
    @BindView(R.id.btn_login) lateinit var button: CircularProgressButton
    @BindView(R.id.no_account) lateinit var noAccountView: TextView

    private var createAccount = false

    companion object {
        fun getIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)

        noAccountView.setOnClickListener {
            toggleCreateAccountMode()
        }
    }

    private fun toggleCreateAccountMode() {
        TransitionManager.beginDelayedTransition(findViewById(R.id.container))
        createAccount = !createAccount
        if(createAccount) {
            emailLayout.show()
            noAccountView.setText(R.string.signing_prompt)
            button.setText(R.string.create_account)
        } else {
            emailLayout.hide()
            noAccountView.setText(R.string.create_account_prompt)
            button.setText(R.string.login_to_ogs)
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
        if(button.visibility == View.VISIBLE) {
            button.doneLoadingAnimation(resources.getColor(R.color.colorAccent), BitmapFactory.decodeResource(resources, R.mipmap.white_check))
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
        button.visibility = View.VISIBLE
        usernameLayout.visibility = View.VISIBLE
        passwordLayout.visibility = View.VISIBLE
        noAccountView.visibility = View.VISIBLE
//        username.postDelayed({
//            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            inputMethodManager.toggleSoftInputFromWindow(
//                    username.applicationWindowToken,
//                    InputMethodManager.SHOW_IMPLICIT, 0)
//            username.requestFocus()
//        }, 100)
    }

    private fun onPasswordLoginFailure(t: Throwable) {
        button.revertAnimation()
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        if(t is HttpException && t.code() == 401) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Login failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    @OnClick(R.id.btn_login)
    fun onLoginClicked() {
        button.startAnimation()
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
                .subscribe(
                        this::onLoginSuccess,
                        this::onPasswordLoginFailure
                )
    }

    private fun onCreateAccountSuccess() {
        doLogin()
    }

    private fun onCreateAccountFailure(t: Throwable) {
        button.revertAnimation {
            button.setText(R.string.create_account)
        }
        Log.e(LoginActivity::class.java.simpleName, t.message, t)
        if(t is HttpException && t.response().errorBody() != null) {
            val error = JSONObject(t.response().errorBody()?.string())["error"].toString()
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Create Account failed. Debug info: '${t.message}'", Toast.LENGTH_LONG).show()
        }
    }

    @OnTextChanged(R.id.input_username, R.id.input_password)
    fun onTextChanged() {
        button.isEnabled = username.text.isNotEmpty() && password.text.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        button.dispose()
    }
}