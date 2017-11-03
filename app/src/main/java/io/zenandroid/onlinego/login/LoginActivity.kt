package io.zenandroid.onlinego.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnTextChanged
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ogs.OGSService
import org.json.JSONArray
import org.json.JSONObject



/**
 * Created by alex on 02/11/2017.
 */
class LoginActivity : AppCompatActivity() {

    val TAG = LoginActivity::class.java.name

    @BindView(R.id.input_username) lateinit var username: TextInputEditText
    @BindView(R.id.input_password) lateinit var password: TextInputEditText
    @BindView(R.id.btn_login) lateinit var button: Button

    companion object {
        fun getIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.btn_login)
    fun onLoginClicked() {
        OGSService.instance.login(username.text.toString(), password.text.toString())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
//                    Handler().postDelayed({OGSService.instance.disconnect()}, 15000)

                    println("success")
                    finish()


                }, {Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()})
    }

    @OnTextChanged(R.id.input_username, R.id.input_password)
    fun onTextChanged() {
        button.isEnabled = username.text.isNotEmpty() && password.text.isNotEmpty()
    }
}

fun createJsonObject(func: JSONObject.() -> Unit): JSONObject {
    val obj = JSONObject()
    func(obj)
    return obj
}

fun createJsonArray(func: JSONArray.() -> Unit): JSONArray {
    val obj = JSONArray()
    func(obj)
    return obj
}