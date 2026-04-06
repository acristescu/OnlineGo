package io.zenandroid.onlinego.ui.screens.face2face.session

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.BindException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

private const val HOST_BIND_RETRY_DELAY_MS = 250L
private const val HOST_BIND_RETRY_ATTEMPTS = 6
private const val SOCKET_READ_TIMEOUT_MS = 15_000

interface FaceToFacePeerConnectionManager {
  suspend fun host(
    port: Int = FaceToFaceLanConnectionManager.DEFAULT_PORT,
    onClosed: (Throwable?) -> Unit = {},
  ): FaceToFaceLanHostHandle

  suspend fun join(
    hostAddress: String,
    port: Int = FaceToFaceLanConnectionManager.DEFAULT_PORT,
    onClosed: (Throwable?) -> Unit = {},
  ): FaceToFaceTransport
}

class FaceToFaceLanConnectionManager : FaceToFacePeerConnectionManager {
  override suspend fun host(
    port: Int,
    onClosed: (Throwable?) -> Unit,
  ): FaceToFaceLanHostHandle = withContext(Dispatchers.IO) {
    val serverSocket = retryAddressInUse {
      // Configure SO_REUSEADDR before bind so the host can re-bind quickly after a disconnect.
      ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(port))
      }
    }

    FaceToFaceLanHostHandle(
      localAddress = resolveLocalIpv4Address(),
      port = serverSocket.localPort,
      awaitTransport = {
        val socket = serverSocket.accept()
        runCatching { serverSocket.close() }
        FaceToFaceLanSocketTransport(socket, onClosed)
      },
      closeServer = {
        runCatching { serverSocket.close() }
      }
    )
  }

  override suspend fun join(
    hostAddress: String,
    port: Int,
    onClosed: (Throwable?) -> Unit,
  ): FaceToFaceTransport = withContext(Dispatchers.IO) {
    val socket = Socket()
    socket.connect(InetSocketAddress(hostAddress, port), CONNECT_TIMEOUT_MS)
    FaceToFaceLanSocketTransport(socket, onClosed)
  }

  private fun resolveLocalIpv4Address(): String {
    val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }
      .getOrElse { emptyList() }
    val candidates = mutableListOf<FaceToFaceLanAddressCandidate>()

    interfaces.forEach { networkInterface ->
      val usable = runCatching { networkInterface.isUp && !networkInterface.isLoopback }
        .getOrDefault(false)
      if (!usable) return@forEach

      val isVirtual = runCatching { networkInterface.isVirtual }.getOrDefault(false)
      val isPointToPoint = runCatching { networkInterface.isPointToPoint }.getOrDefault(false)
      val displayName = runCatching { networkInterface.displayName }.getOrNull()

      Collections.list(networkInterface.inetAddresses)
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress }
        .forEach { address ->
          val hostAddress = address.hostAddress?.trim().orEmpty()
          if (hostAddress.isBlank()) return@forEach

          candidates += FaceToFaceLanAddressCandidate(
            interfaceName = networkInterface.name.orEmpty(),
            displayName = displayName,
            hostAddress = hostAddress,
            isSiteLocal = address.isSiteLocalAddress,
            isLinkLocal = address.isLinkLocalAddress,
            isVirtual = isVirtual,
            isPointToPoint = isPointToPoint,
          )
        }
    }

    return selectBestLanHostAddress(candidates)
  }

  companion object {
    const val DEFAULT_PORT = 45123

    private const val CONNECT_TIMEOUT_MS = 5_000
  }
}

class FaceToFaceLanHostHandle internal constructor(
  val localAddress: String,
  val port: Int,
  private val awaitTransport: suspend () -> FaceToFaceTransport,
  private val closeServer: suspend () -> Unit,
) {
  suspend fun awaitTransport(): FaceToFaceTransport = awaitTransport.invoke()

  suspend fun close() {
    closeServer.invoke()
  }
}

private class FaceToFaceLanSocketTransport(
  private val socket: Socket,
  private val onClosed: (Throwable?) -> Unit,
) : FaceToFaceTransport {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val closed = AtomicBoolean(false)
  private val incoming = Channel<FaceToFacePeerMessage>(capacity = Channel.UNLIMITED)
  private val reader = socket.getInputStream().bufferedReader()
  private val writer = socket.getOutputStream().bufferedWriter()

  override val incomingMessages = incoming.receiveAsFlow()

  init {
    socket.soTimeout = SOCKET_READ_TIMEOUT_MS
    scope.launch {
      try {
        while (true) {
          val payload = reader.readLine() ?: break
          FaceToFacePeerMessageJsonCodec.decode(payload)?.let { incoming.send(it) }
        }
        closeInternal(null)
      } catch (e: Exception) {
        closeInternal(e)
      }
    }
  }

  override suspend fun send(message: FaceToFacePeerMessage) {
    withContext(Dispatchers.IO) {
      val payload = FaceToFacePeerMessageJsonCodec.encode(message)
      writer.write(payload)
      writer.newLine()
      writer.flush()
    }
  }

  override suspend fun close() {
    closeInternal(null)
  }

  private suspend fun closeInternal(cause: Throwable?) {
    if (!closed.compareAndSet(false, true)) return

    incoming.close(cause)
    runCatching { reader.close() }
    runCatching { writer.close() }
    runCatching { socket.close() }
    onClosed(cause)
    scope.cancel()
  }
}

internal suspend fun <T> retryAddressInUse(
  attempts: Int = HOST_BIND_RETRY_ATTEMPTS,
  delayMs: Long = HOST_BIND_RETRY_DELAY_MS,
  block: () -> T,
): T {
  require(attempts > 0) { "attempts must be greater than 0" }

  repeat(attempts - 1) {
    try {
      return block()
    } catch (error: Throwable) {
      if (!isAddressInUseError(error)) throw error
      delay(delayMs)
    }
  }

  return block()
}

internal fun isAddressInUseError(error: Throwable): Boolean {
  return error is BindException || error.message?.contains("EADDRINUSE", ignoreCase = true) == true
}

internal data class FaceToFaceLanAddressCandidate(
  val interfaceName: String,
  val displayName: String?,
  val hostAddress: String,
  val isSiteLocal: Boolean,
  val isLinkLocal: Boolean,
  val isVirtual: Boolean,
  val isPointToPoint: Boolean,
)

internal fun selectBestLanHostAddress(
  candidates: List<FaceToFaceLanAddressCandidate>,
): String {
  return candidates
    .asSequence()
    .filterNot { it.hostAddress.startsWith("127.") || it.isLinkLocal }
    .filterNot { it.isClearlyTunnelInterface() }
    .maxByOrNull { candidate ->
      var score = 0
      if (candidate.isSiteLocal) score += 100
      if (candidate.isLikelyWireless()) score += 40
      if (candidate.isLikelyEthernet()) score += 30
      if (!candidate.isVirtual) score += 20
      if (!candidate.isPointToPoint) score += 10
      score
    }
    ?.hostAddress
    ?: "127.0.0.1"
}

private fun FaceToFaceLanAddressCandidate.isClearlyTunnelInterface(): Boolean {
  val value = "${interfaceName.lowercase()} ${displayName.orEmpty().lowercase()}"
  return listOf(
    "tun",
    "tap",
    "tailscale",
    "wg",
    "wireguard",
    "docker",
    "veth",
    "br-",
    "virbr",
    "zt",
    "ppp",
    "vpn",
  ).any(value::contains)
}

private fun FaceToFaceLanAddressCandidate.isLikelyWireless(): Boolean {
  val value = "${interfaceName.lowercase()} ${displayName.orEmpty().lowercase()}"
  return listOf("wlan", "wifi", "wi-fi", "wlp", "ap", "swlan").any(value::contains)
}

private fun FaceToFaceLanAddressCandidate.isLikelyEthernet(): Boolean {
  val value = "${interfaceName.lowercase()} ${displayName.orEmpty().lowercase()}"
  return listOf("eth", "enp", "eno").any(value::contains)
}

private object FaceToFacePeerMessageJsonCodec {
  private val adapter = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
    .adapter(FaceToFacePeerWireMessage::class.java)

  fun encode(message: FaceToFacePeerMessage): String {
    return adapter.toJson(message.toWire())
  }

  fun decode(payload: String): FaceToFacePeerMessage? {
    return runCatching { adapter.fromJson(payload) }
      .getOrNull()
      ?.toDomain()
  }

  private fun FaceToFacePeerMessage.toWire(): FaceToFacePeerWireMessage {
    return when (this) {
      is FaceToFacePeerMessage.Hello -> FaceToFacePeerWireMessage(
        type = "hello",
        sessionId = sessionId,
        protocolVersion = protocolVersion,
        deviceName = deviceName,
        supportsUndo = supportsUndo,
      )

      is FaceToFacePeerMessage.StartGame -> FaceToFacePeerWireMessage(
        type = "start_game",
        sessionId = sessionId,
        snapshot = snapshot,
      )

      is FaceToFacePeerMessage.Move -> FaceToFacePeerWireMessage(
        type = "move",
        sessionId = sessionId,
        moveNumber = moveNumber,
        player = player,
        cell = cell,
      )

      is FaceToFacePeerMessage.SyncRequest -> FaceToFacePeerWireMessage(
        type = "sync_request",
        sessionId = sessionId,
        expectedMoveCount = expectedMoveCount,
      )

      is FaceToFacePeerMessage.SyncState -> FaceToFacePeerWireMessage(
        type = "sync_state",
        sessionId = sessionId,
        snapshot = snapshot,
      )

      is FaceToFacePeerMessage.UndoRequest -> FaceToFacePeerWireMessage(
        type = "undo_request",
        sessionId = sessionId,
        rollbackToMoveCount = rollbackToMoveCount,
      )

      is FaceToFacePeerMessage.UndoResponse -> FaceToFacePeerWireMessage(
        type = "undo_response",
        sessionId = sessionId,
        accepted = accepted,
        rollbackToMoveCount = rollbackToMoveCount,
      )

      is FaceToFacePeerMessage.Resign -> FaceToFacePeerWireMessage(
        type = "resign",
        sessionId = sessionId,
        player = player,
      )

      is FaceToFacePeerMessage.KeepAlive -> FaceToFacePeerWireMessage(
        type = "keep_alive",
        sessionId = sessionId,
        moveCount = moveCount,
      )
    }
  }

  private fun FaceToFacePeerWireMessage.toDomain(): FaceToFacePeerMessage? {
    return when (type) {
      "hello" -> if (deviceName != null && protocolVersion != null) {
        FaceToFacePeerMessage.Hello(
          sessionId = sessionId,
          protocolVersion = protocolVersion,
          deviceName = deviceName,
          supportsUndo = supportsUndo ?: true,
        )
      } else null

      "start_game" -> snapshot?.let {
        FaceToFacePeerMessage.StartGame(sessionId = sessionId, snapshot = it)
      }

      "move" -> if (moveNumber != null && player != null && cell != null) {
        FaceToFacePeerMessage.Move(
          sessionId = sessionId,
          moveNumber = moveNumber,
          player = player,
          cell = cell,
        )
      } else null

      "sync_request" -> expectedMoveCount?.let {
        FaceToFacePeerMessage.SyncRequest(sessionId = sessionId, expectedMoveCount = it)
      }

      "sync_state" -> snapshot?.let {
        FaceToFacePeerMessage.SyncState(sessionId = sessionId, snapshot = it)
      }

      "undo_request" -> rollbackToMoveCount?.let {
        FaceToFacePeerMessage.UndoRequest(sessionId = sessionId, rollbackToMoveCount = it)
      }

      "undo_response" -> if (accepted != null && rollbackToMoveCount != null) {
        FaceToFacePeerMessage.UndoResponse(
          sessionId = sessionId,
          accepted = accepted,
          rollbackToMoveCount = rollbackToMoveCount,
        )
      } else null

      "resign" -> player?.let {
        FaceToFacePeerMessage.Resign(sessionId = sessionId, player = it)
      }

      "keep_alive" -> moveCount?.let {
        FaceToFacePeerMessage.KeepAlive(sessionId = sessionId, moveCount = it)
      }

      else -> null
    }
  }
}

private data class FaceToFacePeerWireMessage(
  val type: String,
  val sessionId: String,
  val protocolVersion: Int? = null,
  val deviceName: String? = null,
  val supportsUndo: Boolean? = null,
  val snapshot: FaceToFaceGameSnapshot? = null,
  val moveNumber: Int? = null,
  val player: StoneType? = null,
  val cell: Cell? = null,
  val expectedMoveCount: Int? = null,
  val rollbackToMoveCount: Int? = null,
  val accepted: Boolean? = null,
  val moveCount: Int? = null,
)
