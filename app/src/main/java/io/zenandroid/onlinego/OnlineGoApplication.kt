package io.zenandroid.onlinego

import android.app.Application
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.bundled.BundledEmojiCompatConfig




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

        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
    }
}