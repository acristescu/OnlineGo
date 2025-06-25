package io.zenandroid.onlinego.data.ogs

import android.os.Build
import com.google.android.gms.common.util.IOUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.recordException
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.util.zip.GZIPInputStream


private class EmulatorDnsSelector : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
    }
}

class HTTPConnectionFactory(
        private val userSessionRepository: UserSessionRepository
) {
    fun buildConnection() =
        OkHttpClient.Builder()
            .run { if(isEmulator()) dns(EmulatorDnsSelector()) else this }
            .followRedirects(false)
            .cookieJar(userSessionRepository.cookieJar)
            .addNetworkInterceptor { chain ->
                var request = chain.request()
                val csrftoken = userSessionRepository.cookieJar.loadForRequest(request.url).firstOrNull { it.name == "csrftoken" }?.value
                request = request.newBuilder()
                        .addHeader("referer", BuildConfig.BASE_URL + "/overview")
                        .apply { csrftoken?.let { addHeader("x-csrftoken",  it) } }
                        .apply {
                            if(request.url.pathSegments.contains("godojo")) {
                                userSessionRepository.uiConfig?.user_jwt?.let {
                                    addHeader("X-User-Info", it)
                                }
                            }
                        }
                        .build()

                val hasSessionCookieInJar = userSessionRepository.cookieJar.loadForRequest(request.url).any { it.name == "sessionid" }
                val isSessionCookieExpired = userSessionRepository.cookieJar.loadForRequest(request.url).any { it.name == "sessionid" && it.expiresAt < System.currentTimeMillis() }

                val response = chain.proceed(request)

                if(response.isSuccessful) {
                    FirebaseCrashlytics.getInstance().log("HTTP REQUEST ${request.method} ${request.url} -> ${response.code}")
                } else {
                    val sessionCookieSent = request.header("Cookie")?.contains("sessionid=") == true

                    val csrftokenInfo = if(csrftoken == null) "no csrf" else "csrf present"
                    val cookieJarInfo = when {
                        isSessionCookieExpired -> "expired session cookie"
                        hasSessionCookieInJar -> "session cookie in jar"
                        else -> "no session cookie"
                    }
                    val sessionCookieInfo = if(sessionCookieSent) "session cookie sent" else "session cookie not sent"
                    FirebaseCrashlytics.getInstance().log("E/HTTP_REQUEST: ${request.method} ${request.url} -> ${response.code} ${response.message} [$cookieJarInfo] [$csrftokenInfo] [$sessionCookieInfo] ${peekBody(response)}")

                    if(!sessionCookieSent && hasSessionCookieInJar && !isSessionCookieExpired) {
                        recordException(Exception("Possible cookie jar problem"))
                    }
                }
                response
            }
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

    private fun peekBody(response: okhttp3.Response) = try {
        val bodyBytes = response.peekBody(1024 * 1024).bytes()
        if(IOUtils.isGzipByteBuffer(bodyBytes))
            String(IOUtils.toByteArray(GZIPInputStream(ByteArrayInputStream(bodyBytes))))
        else
            String(bodyBytes)
    } catch (t: Throwable) {
        recordException(t)
        "<<<Error trying to log body of response ${t.javaClass.name} ${t.message}>>>"
    }
}