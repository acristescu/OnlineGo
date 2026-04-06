package io.zenandroid.onlinego.ui.screens.face2face.session

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FACE_TO_FACE_LAN_SERVICE_TYPE = "_onlinego-f2f._tcp."
private const val FACE_TO_FACE_LAN_SERVICE_PREFIX = "OnlineGo F2F"
private const val FACE_TO_FACE_LAN_SERVICE_NAME_MAX_LENGTH = 63

data class FaceToFaceLanDiscoveredHost(
  val displayName: String,
  val host: String,
  val port: Int,
) {
  val endpoint: String
    get() = "$host:$port"
}

interface FaceToFaceLanDiscoveryManager {
  fun discoverHosts(): Flow<List<FaceToFaceLanDiscoveredHost>>

  suspend fun startAdvertising(
    sessionId: String,
    deviceName: String,
    port: Int,
  )

  suspend fun stopAdvertising()
}

class NoOpFaceToFaceLanDiscoveryManager : FaceToFaceLanDiscoveryManager {
  override fun discoverHosts(): Flow<List<FaceToFaceLanDiscoveredHost>> = flowOf(emptyList())

  override suspend fun startAdvertising(sessionId: String, deviceName: String, port: Int) = Unit

  override suspend fun stopAdvertising() = Unit
}

class AndroidFaceToFaceLanDiscoveryManager(
  context: Context,
) : FaceToFaceLanDiscoveryManager {
  private val appContext = context.applicationContext
  private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

  @Volatile
  private var registrationListener: NsdManager.RegistrationListener? = null

  override fun discoverHosts(): Flow<List<FaceToFaceLanDiscoveredHost>> = callbackFlow {
    val discoveredHosts = linkedMapOf<String, FaceToFaceLanDiscoveredHost>()
    val listener = object : NsdManager.DiscoveryListener {
      override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        close(IllegalStateException("LAN discovery start failed: $errorCode"))
      }

      override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        close(IllegalStateException("LAN discovery stop failed: $errorCode"))
      }

      override fun onDiscoveryStarted(serviceType: String) = Unit

      override fun onDiscoveryStopped(serviceType: String) = Unit

      override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        discoveredHosts.remove(serviceInfo.serviceName)
        trySend(discoveredHosts.values.sortedBy { it.displayName.lowercase() })
      }

      override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        if (serviceInfo.serviceType != FACE_TO_FACE_LAN_SERVICE_TYPE) return
        if (!serviceInfo.serviceName.startsWith(FACE_TO_FACE_LAN_SERVICE_PREFIX)) return

        nsdManager.resolveService(
          serviceInfo,
          object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

            override fun onServiceResolved(resolved: NsdServiceInfo) {
              val hostAddress = resolved.host?.hostAddress?.takeIf { it.isNotBlank() } ?: return
              discoveredHosts[resolved.serviceName] = FaceToFaceLanDiscoveredHost(
                displayName = displayNameFromLanServiceName(resolved.serviceName),
                host = hostAddress,
                port = resolved.port,
              )
              trySend(discoveredHosts.values.sortedBy { it.displayName.lowercase() })
            }
          }
        )
      }
    }

    nsdManager.discoverServices(
      FACE_TO_FACE_LAN_SERVICE_TYPE,
      NsdManager.PROTOCOL_DNS_SD,
      listener,
    )

    awaitClose {
      runCatching { nsdManager.stopServiceDiscovery(listener) }
    }
  }

  override suspend fun startAdvertising(
    sessionId: String,
    deviceName: String,
    port: Int,
  ) {
    stopAdvertising()

    val serviceInfo = NsdServiceInfo().apply {
      serviceType = FACE_TO_FACE_LAN_SERVICE_TYPE
      serviceName = buildLanServiceName(deviceName, sessionId)
      setPort(port)
    }

    val listener = suspendCancellableCoroutine<NsdManager.RegistrationListener> { continuation ->
      val callback = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
          if (continuation.isActive) {
            continuation.resumeWithException(
              IllegalStateException("LAN advertising failed: $errorCode")
            )
          }
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
          if (continuation.isActive) continuation.resume(this)
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
      }

      nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, callback)

      continuation.invokeOnCancellation {
        runCatching { nsdManager.unregisterService(callback) }
      }
    }

    registrationListener = listener
  }

  override suspend fun stopAdvertising() {
    val listener = registrationListener ?: return
    registrationListener = null
    runCatching { nsdManager.unregisterService(listener) }
  }
}

internal fun buildLanServiceName(deviceName: String, sessionId: String): String {
  val sanitizedDeviceName = deviceName
    .replace(Regex("[^A-Za-z0-9 _-]"), "")
    .trim()
    .takeIf { it.isNotBlank() }
    ?: "Android"
  val suffix = sessionId
    .replace(Regex("[^A-Za-z0-9]"), "")
    .take(6)
    .ifBlank { "session" }
  val reservedLength = FACE_TO_FACE_LAN_SERVICE_PREFIX.length + suffix.length + 2
  val maxDeviceNameLength = (FACE_TO_FACE_LAN_SERVICE_NAME_MAX_LENGTH - reservedLength).coerceAtLeast(1)
  val truncatedDeviceName = sanitizedDeviceName
    .take(maxDeviceNameLength)
    .trim()
    .ifBlank { "Android" }
  return "$FACE_TO_FACE_LAN_SERVICE_PREFIX $truncatedDeviceName $suffix"
}

internal fun displayNameFromLanServiceName(serviceName: String): String {
  val payload = serviceName.removePrefix("$FACE_TO_FACE_LAN_SERVICE_PREFIX ").trim()
  if (payload.isBlank() || payload == serviceName) return "Host"
  return payload.substringBeforeLast(' ').takeIf { it.isNotBlank() } ?: "Host"
}
