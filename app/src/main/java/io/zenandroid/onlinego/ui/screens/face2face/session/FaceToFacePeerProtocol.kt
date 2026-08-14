package io.zenandroid.onlinego.ui.screens.face2face.session

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import kotlinx.coroutines.flow.Flow

const val FACE_TO_FACE_PROTOCOL_VERSION = 1

@Immutable
data class FaceToFaceGameConfig(
  val boardSize: Int = 19,
  val handicap: Int = 0,
) {
  init {
    require(boardSize in SUPPORTED_BOARD_SIZES) {
      "Unsupported board size=$boardSize. Supported sizes=$SUPPORTED_BOARD_SIZES"
    }
    require(handicap in 0..9) { "Handicap must be between 0 and 9" }
  }

  companion object {
    val SUPPORTED_BOARD_SIZES = setOf(9, 13, 19)
  }
}

enum class FaceToFaceSessionMode {
  HOTSEAT,
  PEER_TO_PEER,
}

enum class FaceToFacePeerRole {
  HOST,
  GUEST,
}

enum class FaceToFaceTransportType {
  WIFI_LAN,
  BLUETOOTH,
}

enum class FaceToFaceConnectionState {
  DISCONNECTED,
  HOSTING,
  CONNECTING,
  CONNECTED,
  SYNCING,
}

@Immutable
data class FaceToFaceGameSnapshot(
  val protocolVersion: Int = FACE_TO_FACE_PROTOCOL_VERSION,
  val sessionId: String,
  val config: FaceToFaceGameConfig,
  val hostPlaysBlack: Boolean = true,
  val moveHistory: List<Cell> = emptyList(),
)

@Immutable
sealed interface FaceToFacePeerMessage {
  val sessionId: String

  data class Hello(
    override val sessionId: String,
    val protocolVersion: Int = FACE_TO_FACE_PROTOCOL_VERSION,
    val deviceName: String,
    val supportsUndo: Boolean = true,
  ) : FaceToFacePeerMessage

  data class StartGame(
    override val sessionId: String,
    val snapshot: FaceToFaceGameSnapshot,
  ) : FaceToFacePeerMessage

  data class Move(
    override val sessionId: String,
    val moveNumber: Int,
    val player: StoneType,
    val cell: Cell,
  ) : FaceToFacePeerMessage

  data class SyncRequest(
    override val sessionId: String,
    val expectedMoveCount: Int,
  ) : FaceToFacePeerMessage

  data class SyncState(
    override val sessionId: String,
    val snapshot: FaceToFaceGameSnapshot,
  ) : FaceToFacePeerMessage

  data class UndoRequest(
    override val sessionId: String,
    val rollbackToMoveCount: Int,
  ) : FaceToFacePeerMessage

  data class UndoResponse(
    override val sessionId: String,
    val accepted: Boolean,
    val rollbackToMoveCount: Int,
  ) : FaceToFacePeerMessage

  data class Resign(
    override val sessionId: String,
    val player: StoneType,
  ) : FaceToFacePeerMessage

  data class KeepAlive(
    override val sessionId: String,
    val moveCount: Int,
  ) : FaceToFacePeerMessage
}

interface FaceToFaceTransport {
  val incomingMessages: Flow<FaceToFacePeerMessage>

  suspend fun send(message: FaceToFacePeerMessage)

  suspend fun close()
}
