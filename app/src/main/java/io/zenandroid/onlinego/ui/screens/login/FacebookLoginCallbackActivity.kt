package io.zenandroid.onlinego.ui.screens.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject

class FacebookLoginCallbackActivity : AppCompatActivity() {

    private val userSessionRepository: UserSessionRepository by inject()
    private val ogsRestService: OGSRestService by inject()

    private val client = OkHttpClient.Builder()
        .cookieJar(userSessionRepository.cookieJar)
        .followRedirects(false)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.data
        if(url != null) {
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            Completable.fromCallable { client.newCall(request).execute() }
                .andThen(ogsRestService.fetchUIConfig())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoginSuccess, this::onLoginFailed)
        } else {
            onLoginFailed(Exception("Login failed"))
        }
    }

    fun onLoginSuccess() {
        startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
        finish()
    }

    fun onLoginFailed(t: Throwable) {
        startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
        finish()
    }
}