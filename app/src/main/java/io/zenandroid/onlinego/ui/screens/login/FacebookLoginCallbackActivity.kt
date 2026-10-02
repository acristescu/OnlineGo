package io.zenandroid.onlinego.ui.screens.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class FacebookLoginCallbackActivity : ComponentActivity() {

    private val httpClient: HttpClient by inject()
    private val ogsRestService: OGSRestService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.data
        if(url != null) {
            lifecycleScope.launch {
                try {
                    httpClient.get(url.toString()) { expectSuccess = false }
                    ogsRestService.fetchUIConfig()
                    onLoginSuccess()
                } catch (e: Exception) {
                    onLoginFailed(e)
                }
            }
        } else {
            onLoginFailed(Exception("Login failed"))
        }
    }

    private fun onLoginSuccess() {
        startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
        finish()
    }

    private fun onLoginFailed(t: Throwable) {
        startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) })
        finish()
    }
}
