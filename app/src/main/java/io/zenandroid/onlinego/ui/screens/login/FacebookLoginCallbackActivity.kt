package io.zenandroid.onlinego.ui.screens.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FacebookLoginCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, LoginActivity::class.java).apply { data = intent?.data })
    }
}