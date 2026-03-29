package io.zenandroid.onlinego.ui.screens.face2face.session

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class FaceToFaceSessionEngineTest {
  private val engine = FaceToFaceSessionEngine()

  @Test
  fun `hotseat session starts from rules defaults`() = runTest {
    val state = engine.createHotseatSession(
      config = FaceToFaceGameConfig(boardSize = 9, handicap = 2)
    )

    assertEquals(9, state.position.boardWidth)
    assertEquals(2, state.position.blackStones.size)
    assertEquals(StoneType.WHITE, state.position.nextToMove)
    assertTrue(state.isLocalTurn)
  }

  @Test
  fun `local move updates history and position`() = runTest {
    val initial = engine.createHotseatSession(FaceToFaceGameConfig(boardSize = 19, handicap = 0))

    val result = engine.applyLocalMove(initial, Cell(3, 3))

    result as FaceToFaceSessionMutationResult.Applied
    assertEquals(listOf(Cell(3, 3)), result.state.moveHistory)
    assertEquals(1, result.state.position.blackStones.size)
    assertEquals(StoneType.WHITE, result.state.position.nextToMove)
    assertEquals(1, result.state.positionHistory.size)
  }

  @Test
  fun `remote move respects turn ownership and move numbering`() = runTest {
    val initial = engine.createPeerSession(
      config = FaceToFaceGameConfig(boardSize = 19, handicap = 0),
      localRole = FaceToFacePeerRole.GUEST,
      transport = FaceToFaceTransportType.WIFI_LAN,
    )

    val remoteMove = FaceToFacePeerMessage.Move(
      sessionId = initial.sessionId,
      moveNumber = 1,
      player = StoneType.BLACK,
      cell = Cell(15, 15),
    )

    val result = engine.applyRemoteMove(initial, remoteMove)

    result as FaceToFaceSessionMutationResult.Applied
    assertEquals(listOf(Cell(15, 15)), result.state.moveHistory)
    assertEquals(StoneType.WHITE, result.state.position.nextToMove)
    assertTrue(result.state.isLocalTurn)
  }

  @Test
  fun `out of sync remote move is rejected`() = runTest {
    val initial = engine.createPeerSession(
      config = FaceToFaceGameConfig(boardSize = 19, handicap = 0),
      localRole = FaceToFacePeerRole.GUEST,
      transport = FaceToFaceTransportType.WIFI_LAN,
    )

    val remoteMove = FaceToFacePeerMessage.Move(
      sessionId = initial.sessionId,
      moveNumber = 2,
      player = StoneType.BLACK,
      cell = Cell(15, 15),
    )

    val result = engine.applyRemoteMove(initial, remoteMove)

    result as FaceToFaceSessionMutationResult.Rejected
    assertEquals(FaceToFaceMoveRejectReason.OUT_OF_SYNC, result.reason)
  }

  @Test
  fun `snapshot round trip restores move history and board state`() = runTest {
    val initial = engine.createHotseatSession(FaceToFaceGameConfig(boardSize = 13, handicap = 0))
    val afterBlack = engine.applyLocalMove(initial, Cell(3, 3)) as FaceToFaceSessionMutationResult.Applied
    val afterWhite = engine.applyLocalMove(afterBlack.state, Cell(9, 9)) as FaceToFaceSessionMutationResult.Applied

    val snapshot = engine.toSnapshot(afterWhite.state)
    val restored = engine.restoreFromSnapshot(snapshot)

    assertEquals(afterWhite.state.moveHistory, restored.moveHistory)
    assertEquals(afterWhite.state.position.blackStones, restored.position.blackStones)
    assertEquals(afterWhite.state.position.whiteStones, restored.position.whiteStones)
    assertEquals(afterWhite.state.position.nextToMove, restored.position.nextToMove)
  }

  @Test
  fun `ko attempt is rejected`() = runTest {
    val moves = "E5, D6, E7, E8, E6, F6, F7, D7, G6, F8, F5, F4, G5, H5, E4, H6, G4, H4, F3, D5, E3, D3, D4, C4, C6, E2, C5, F2, C7, C8, D8, D9, D7, B7, B8, B6, C9, B9, E9, F9, G8, H8, G9, H9, H7, J7, J8, J9, J8, J6, A9, B5, B4, A3, A4, B3, A5, A6, A7, A8, A7"
      .split(", ")
      .map { Cell.fromGTP(it, 9) }

    var state = engine.createHotseatSession(FaceToFaceGameConfig(boardSize = 9, handicap = 0))

    moves.dropLast(1).forEach { cell ->
      val result = engine.applyLocalMove(state, cell) as FaceToFaceSessionMutationResult.Applied
      state = result.state
    }

    val koAttempt = engine.applyLocalMove(state, moves.last())

    koAttempt as FaceToFaceSessionMutationResult.Rejected
    assertEquals(FaceToFaceMoveRejectReason.KO, koAttempt.reason)
    assertEquals(moves.dropLast(1), koAttempt.state.moveHistory)
  }
}
