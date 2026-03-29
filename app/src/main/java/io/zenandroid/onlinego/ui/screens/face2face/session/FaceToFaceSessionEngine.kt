package io.zenandroid.onlinego.ui.screens.face2face.session

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.StoneType.BLACK
import io.zenandroid.onlinego.data.model.StoneType.WHITE
import io.zenandroid.onlinego.gamelogic.RulesManager
import java.util.UUID

@Immutable
data class FaceToFaceSessionState(
  val sessionId: String,
  val mode: FaceToFaceSessionMode,
  val localRole: FaceToFacePeerRole,
  val hostPlaysBlack: Boolean,
  val transport: FaceToFaceTransportType?,
  val connectionState: FaceToFaceConnectionState,
  val config: FaceToFaceGameConfig,
  val position: Position,
  val moveHistory: List<Cell>,
  val positionHistory: List<Position>,
  val localPlayerName: String,
  val remotePlayerName: String?,
  val lastError: String? = null,
) {
  val localStone: StoneType?
    get() = when (mode) {
      FaceToFaceSessionMode.HOTSEAT -> null
      FaceToFaceSessionMode.PEER_TO_PEER -> when {
        localRole == FaceToFacePeerRole.HOST && hostPlaysBlack -> BLACK
        localRole == FaceToFacePeerRole.GUEST && !hostPlaysBlack -> BLACK
        else -> WHITE
      }
    }

  val remoteStone: StoneType?
    get() = when (localStone) {
      BLACK -> WHITE
      WHITE -> BLACK
      null -> null
    }

  val isLocalTurn: Boolean
    get() = localStone == null || position.nextToMove == localStone
}

enum class FaceToFaceMoveRejectReason {
  OUT_OF_TURN,
  ILLEGAL_MOVE,
  KO,
  OUT_OF_SYNC,
}

sealed interface FaceToFaceSessionMutationResult {
  data class Applied(val state: FaceToFaceSessionState) : FaceToFaceSessionMutationResult
  data class Rejected(
    val state: FaceToFaceSessionState,
    val reason: FaceToFaceMoveRejectReason,
  ) : FaceToFaceSessionMutationResult
}

class FaceToFaceSessionEngine {
  suspend fun createHotseatSession(
    config: FaceToFaceGameConfig,
    sessionId: String = UUID.randomUUID().toString(),
    localPlayerName: String = "Player 1",
    remotePlayerName: String = "Player 2",
  ): FaceToFaceSessionState {
    return newState(
      sessionId = sessionId,
      mode = FaceToFaceSessionMode.HOTSEAT,
      localRole = FaceToFacePeerRole.HOST,
      hostPlaysBlack = true,
      transport = null,
      connectionState = FaceToFaceConnectionState.DISCONNECTED,
      config = config,
      localPlayerName = localPlayerName,
      remotePlayerName = remotePlayerName,
    )
  }

  suspend fun createPeerSession(
    config: FaceToFaceGameConfig,
    localRole: FaceToFacePeerRole,
    transport: FaceToFaceTransportType,
    sessionId: String = UUID.randomUUID().toString(),
    hostPlaysBlack: Boolean = true,
    connectionState: FaceToFaceConnectionState = FaceToFaceConnectionState.CONNECTED,
    localPlayerName: String = "You",
    remotePlayerName: String = "Opponent",
  ): FaceToFaceSessionState {
    return newState(
      sessionId = sessionId,
      mode = FaceToFaceSessionMode.PEER_TO_PEER,
      localRole = localRole,
      hostPlaysBlack = hostPlaysBlack,
      transport = transport,
      connectionState = connectionState,
      config = config,
      localPlayerName = localPlayerName,
      remotePlayerName = remotePlayerName,
    )
  }

  suspend fun restoreFromSnapshot(
    snapshot: FaceToFaceGameSnapshot,
    mode: FaceToFaceSessionMode = FaceToFaceSessionMode.HOTSEAT,
    localRole: FaceToFacePeerRole = FaceToFacePeerRole.HOST,
    transport: FaceToFaceTransportType? = null,
    connectionState: FaceToFaceConnectionState = FaceToFaceConnectionState.DISCONNECTED,
    localPlayerName: String = "Player 1",
    remotePlayerName: String? = if (mode == FaceToFaceSessionMode.HOTSEAT) "Player 2" else "Opponent",
  ): FaceToFaceSessionState {
    require(snapshot.protocolVersion == FACE_TO_FACE_PROTOCOL_VERSION) {
      "Unsupported protocol version=${snapshot.protocolVersion}"
    }

    var state = newState(
      sessionId = snapshot.sessionId,
      mode = mode,
      localRole = localRole,
      hostPlaysBlack = snapshot.hostPlaysBlack,
      transport = transport,
      connectionState = connectionState,
      config = snapshot.config,
      localPlayerName = localPlayerName,
      remotePlayerName = remotePlayerName,
    )

    snapshot.moveHistory.forEach { move ->
      when (val result = applyEngineMove(state, move, state.position.nextToMove)) {
        is FaceToFaceSessionMutationResult.Applied -> state = result.state
        is FaceToFaceSessionMutationResult.Rejected -> {
          throw IllegalArgumentException("Invalid snapshot history: ${result.reason}")
        }
      }
    }

    return state
  }

  fun toSnapshot(state: FaceToFaceSessionState): FaceToFaceGameSnapshot {
    return FaceToFaceGameSnapshot(
      sessionId = state.sessionId,
      config = state.config,
      hostPlaysBlack = state.hostPlaysBlack,
      moveHistory = state.moveHistory,
    )
  }

  suspend fun applyLocalMove(
    state: FaceToFaceSessionState,
    cell: Cell,
  ): FaceToFaceSessionMutationResult {
    if (!state.isLocalTurn) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_TURN)
    }

    return applyEngineMove(state, cell, state.position.nextToMove)
  }

  suspend fun applyRemoteMove(
    state: FaceToFaceSessionState,
    move: FaceToFacePeerMessage.Move,
  ): FaceToFaceSessionMutationResult {
    if (state.mode != FaceToFaceSessionMode.PEER_TO_PEER) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_SYNC)
    }
    if (move.sessionId != state.sessionId) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_SYNC)
    }
    if (move.moveNumber != state.moveHistory.size + 1) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_SYNC)
    }
    if (move.player != state.remoteStone) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_TURN)
    }

    return applyEngineMove(state, move.cell, move.player)
  }

  private suspend fun applyEngineMove(
    state: FaceToFaceSessionState,
    cell: Cell,
    player: StoneType,
  ): FaceToFaceSessionMutationResult {
    if (player != state.position.nextToMove) {
      return reject(state, FaceToFaceMoveRejectReason.OUT_OF_TURN)
    }

    val newPosition = RulesManager.makeMove(state.position, player, cell)
      ?: return reject(state, FaceToFaceMoveRejectReason.ILLEGAL_MOVE)

    if (RulesManager.isIllegalKO(state.positionHistory, newPosition)) {
      return reject(state, FaceToFaceMoveRejectReason.KO)
    }

    return FaceToFaceSessionMutationResult.Applied(
      state.copy(
        position = newPosition,
        moveHistory = state.moveHistory + cell,
        positionHistory = state.positionHistory + newPosition,
        lastError = null,
      )
    )
  }

  private suspend fun newState(
    sessionId: String,
    mode: FaceToFaceSessionMode,
    localRole: FaceToFacePeerRole,
    hostPlaysBlack: Boolean,
    transport: FaceToFaceTransportType?,
    connectionState: FaceToFaceConnectionState,
    config: FaceToFaceGameConfig,
    localPlayerName: String,
    remotePlayerName: String?,
  ): FaceToFaceSessionState {
    return FaceToFaceSessionState(
      sessionId = sessionId,
      mode = mode,
      localRole = localRole,
      hostPlaysBlack = hostPlaysBlack,
      transport = transport,
      connectionState = connectionState,
      config = config,
      position = RulesManager.initializePosition(config.boardSize, config.handicap),
      moveHistory = emptyList(),
      positionHistory = emptyList(),
      localPlayerName = localPlayerName,
      remotePlayerName = remotePlayerName,
    )
  }

  private fun reject(
    state: FaceToFaceSessionState,
    reason: FaceToFaceMoveRejectReason,
  ): FaceToFaceSessionMutationResult.Rejected {
    return FaceToFaceSessionMutationResult.Rejected(
      state = state.copy(lastError = reason.name),
      reason = reason,
    )
  }
}
