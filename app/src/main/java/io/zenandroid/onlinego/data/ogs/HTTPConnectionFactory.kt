package io.zenandroid.onlinego.data.ogs

import android.os.Build
import io.zenandroid.onlinego.BuildConfig
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit


private class EmulatorDnsSelector : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
    }
}

class HTTPConnectionFactory {
    fun buildConnection() =
        OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .run { if(isEmulator()) dns(EmulatorDnsSelector()) else this }
            .followRedirects(false)
            .run { if(BuildConfig.DEBUG) { addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)) } else this }
            .build()

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }
}
