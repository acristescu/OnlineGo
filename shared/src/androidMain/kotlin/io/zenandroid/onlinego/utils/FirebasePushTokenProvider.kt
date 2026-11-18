package io.zenandroid.onlinego.utils

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import io.zenandroid.onlinego.data.repositories.PushToken
import io.zenandroid.onlinego.data.repositories.PushTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FCM_MESSAGE_TYPE = "data"

class FirebasePushTokenProvider : PushTokenProvider {
  override suspend fun token(): PushToken? = try {
    PushToken(value = fetchToken(), fcmMessageType = FCM_MESSAGE_TYPE)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Logger.w("Can't get the FCM token", e, tag = "FirebasePushTokenProvider")
    null
  }

  private suspend fun fetchToken(): String = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token
      .addOnSuccessListener { continuation.resume(it) }
      .addOnFailureListener { continuation.resumeWithException(it) }
  }
}
