package io.zenandroid.onlinego.login

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.transition.Fade
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnTextChanged
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.MainActivity
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ogs.OGSService


/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    @BindView(R.id.input_username) lateinit var username: TextInputEditText
    @BindView(R.id.input_password) lateinit var password: TextInputEditText
    @BindView(R.id.input_username_layout) lateinit var usernameLayout: TextInputLayout
    @BindView(R.id.input_password_layout) lateinit var passwordLayout: TextInputLayout
    @BindView(R.id.logo) lateinit var logo: AppCompatImageView
    @BindView(R.id.btn_login) lateinit var button: CircularProgressButton

    companion object {
        fun getIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)
    }

    override fun onResume() {
        super.onResume()
        OGSService.instance.loginWithToken()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoginSuccess, this::onTokenLoginFailure)
    }

    private fun onLoginSuccess() {
        if(button.visibility == View.VISIBLE) {
            button.doneLoadingAnimation(resources.getColor(R.color.colorAccent), getDoneBitmap())
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
    }

    private fun onPasswordLoginFailure(t: Throwable) {
        button.revertAnimation()
        Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show()
    }

    @OnClick(R.id.btn_login)
    fun onLoginClicked() {
        button.startAnimation()
        OGSService.instance.login(username.text.toString(), password.text.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::onLoginSuccess,
                        this::onPasswordLoginFailure
                )
    }

    @OnTextChanged(R.id.input_username, R.id.input_password)
    fun onTextChanged() {
        button.isEnabled = username.text.isNotEmpty() && password.text.isNotEmpty()
    }

    private fun getDoneBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap((resources.displayMetrics.density * 24).toInt(), (resources.displayMetrics.density * 24).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val drawable = DrawableCompat.wrap(resources.getDrawable(R.drawable.ic_done_24dp))
        drawable.setBounds(0, 0, bmp.width, bmp.height)
        drawable.draw(canvas)
        return bmp
    }
}