package io.zenandroid.onlinego.data.ogs

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.gms.common.util.IOUtils
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class HTTPConnectionFactory(
        private val userSessionRepository: UserSessionRepository
) {
    fun buildConnection() =
        OkHttpClient.Builder()
                .cookieJar(userSessionRepository.cookieJar)
                .addStethoInterceptor()
                .addNetworkInterceptor { chain ->
                    var request = chain.request()
                    val csrftoken = userSessionRepository.cookieJar.loadForRequest(request.url).firstOrNull { it.name == "csrftoken" }?.value
                    request = request.newBuilder()
                            .addHeader("referer", "https://online-go.com/overview")
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
                        //
                        // Note: For some users the server responds with a peculiar answer here, causing Moshi to throw a fit. We will temporarily log this to try and determine
                        // what's going on
                        //
                        if(request.url.encodedPath.endsWith("challenges")) {
                            FirebaseCrashlytics.getInstance().log("HTTP REQUEST ${peekBody(response)}")
                        }
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
                            FirebaseCrashlytics.getInstance().recordException(Exception("Possible cookie jar problem"))
                        }
                    }
                    response
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

    private fun OkHttpClient.Builder.addStethoInterceptor() =
            if(BuildConfig.DEBUG) {
                addNetworkInterceptor(StethoInterceptor())
            } else this

    private fun peekBody(response: okhttp3.Response) = try {
        val bodyBytes = response.peekBody(1024 * 1024).bytes()
        if(IOUtils.isGzipByteBuffer(bodyBytes))
            String(IOUtils.toByteArray(GZIPInputStream(ByteArrayInputStream(bodyBytes))))
        else
            String(bodyBytes)
    } catch (t: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(t)
        "<<<Error trying to log body of response ${t.javaClass.name} ${t.message}>>>"
    }
}