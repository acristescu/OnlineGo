package io.zenandroid.onlinego

import android.app.Application

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
    }
}