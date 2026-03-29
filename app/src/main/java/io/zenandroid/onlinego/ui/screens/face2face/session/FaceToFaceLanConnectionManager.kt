package io.zenandroid.onlinego.ui.screens.face2face.session

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class FaceToFaceLanConnectionManager {
  suspend fun host(
    port: Int = DEFAULT_PORT,
    onClosed: (Throwable?) -> Unit = {},
  ): FaceToFaceLanHostHandle = withContext(Dispatchers.IO) {
    val serverSocket = ServerSocket(port).apply {
      reuseAddress = true
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

  suspend fun join(
    hostAddress: String,
    port: Int = DEFAULT_PORT,
    onClosed: (Throwable?) -> Unit = {},
  ): FaceToFaceTransport = withContext(Dispatchers.IO) {
    val socket = Socket()
    socket.connect(InetSocketAddress(hostAddress, port), CONNECT_TIMEOUT_MS)
    FaceToFaceLanSocketTransport(socket, onClosed)
  }

  private fun resolveLocalIpv4Address(): String {
    val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }
      .getOrElse { emptyList() }

    interfaces.forEach { networkInterface ->
      val usable = runCatching { networkInterface.isUp && !networkInterface.isLoopback }
        .getOrDefault(false)
      if (!usable) return@forEach

      Collections.list(networkInterface.inetAddresses)
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        ?.hostAddress
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }
    }

    return "127.0.0.1"
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
  private val incoming = MutableSharedFlow<FaceToFacePeerMessage>(
    extraBufferCapacity = 32,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  private val reader = socket.getInputStream().bufferedReader()
  private val writer = socket.getOutputStream().bufferedWriter()

  override val incomingMessages = incoming.asSharedFlow()

  init {
    scope.launch {
      try {
        while (true) {
          val payload = reader.readLine() ?: break
          FaceToFacePeerMessageJsonCodec.decode(payload)?.let { incoming.emit(it) }
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

    runCatching { reader.close() }
    runCatching { writer.close() }
    runCatching { socket.close() }
    onClosed(cause)
    scope.cancel()
  }
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
