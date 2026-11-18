package io.zenandroid.onlinego.data.repositories

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.ogs.PushDeviceRequest
import io.zenandroid.onlinego.data.ogs.OGSRestAPI
import io.zenandroid.onlinego.data.ogs.OGS_ENVIRONMENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "PushRegistration"
private const val PUSH_PROVIDER = "fcm"

class PushRegistrationRepository(
  private val restApi: OGSRestAPI,
  private val tokenProvider: PushTokenProvider,
  private val userSessionRepository: UserSessionRepository,
  applicationScope: CoroutineScope,
) {
  init {
    applicationScope.launch {
      userSessionRepository.loginStatus
        .map { (it as? LoginStatus.LoggedIn)?.userId }
        .distinctUntilChanged()
        .filterNotNull()
        .collect { register() }
    }
  }

  suspend fun register() {
    if (!userSessionRepository.isLoggedIn()) return
    val token = tokenProvider.token() ?: return
    try {
      restApi.registerPushDevice(
        PushDeviceRequest(
          clientId = OGS_ENVIRONMENT.pushClientId,
          provider = PUSH_PROVIDER,
          token = token.value,
          fcmMessageType = token.fcmMessageType,
        )
      )
      Logger.i("Registered push device", tag = TAG)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Logger.w("Push device registration failed", e, tag = TAG)
    }
  }

  suspend fun unregister() {
    val token = tokenProvider.token() ?: return
    try {
      restApi.unregisterPushDevice(token.value)
      Logger.i("Unregistered push device", tag = TAG)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Logger.w("Push device unregistration failed", e, tag = TAG)
    }
  }
}

fun interface PushTokenProvider {
  suspend fun token(): PushToken?
}

data class PushToken(val value: String, val fcmMessageType: String)
