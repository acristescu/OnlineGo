package io.zenandroid.onlinego

import android.app.Application
import android.content.ContentValues.TAG
import android.os.Build
import android.support.annotation.Nullable
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.FontRequestEmojiCompatConfig
import android.support.v4.provider.FontRequest
import android.util.Log


/**
 * Created by alex on 04/11/2017.
 */
class OnlineGoApplication : Application() {

    companion object {
        lateinit var instance: OnlineGoApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        //
        // This is a workaround for the downloaded noto being broken on Marshmallow and up
        //
        val fontQuery =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) "Noto Color Emoji Compat"
                else "Intentionaly Broken Query"

        val config: EmojiCompat.Config
        // Use a downloadable font for EmojiCompat
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                fontQuery,
                R.array.com_google_android_gms_fonts_certs)
        config = FontRequestEmojiCompatConfig(applicationContext, fontRequest)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        Log.i(TAG, "EmojiCompat initialized")
                    }

                    override fun onFailed(@Nullable throwable: Throwable?) {
                        Log.e(TAG, "EmojiCompat initialization failed", throwable)
                    }
                })
        EmojiCompat.init(config)
    }
}