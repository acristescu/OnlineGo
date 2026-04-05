package io.zenandroid.onlinego.ui.screens.face2face

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceGameConfig
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceGameSnapshot
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanConnectionManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanHostHandle
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFacePeerConnectionManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFacePeerMessage
import io.zenandroid.onlinego.ui.screens.face2face.session.NoOpFaceToFaceLanDiscoveryManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionEngine
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionState
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FaceToFaceViewModelTest {
  @get:Rule
  val koinTestRule = KoinTestRule.create {
    printLogger(Level.DEBUG)
    modules(allKoinModules)
  }

  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  private val analytics: FirebaseAnalytics = mock()
  private val crashlytics: FirebaseCrashlytics = mock()
  private val settingsRepository: SettingsRepository = mock()
  private val sessionEngine = FaceToFaceSessionEngine()
  private val estimator = FakeFaceToFaceEstimator()
  private lateinit var lanConnectionManager: FakeLanConnectionManager
  private val lanDiscoveryManager = NoOpFaceToFaceLanDiscoveryManager()

  private lateinit var applicationTestScope: TestScope

  private lateinit var viewModel: FaceToFaceViewModel

  @Before
  fun setUp() {
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    applicationTestScope = TestScope(testDispatcher)
    lanConnectionManager = FakeLanConnectionManager()

    whenever(settingsRepository.faceToFaceHistoryFlow).thenReturn(flowOf(null))
    whenever(settingsRepository.faceToFaceBoardSizeFlow).thenReturn(flowOf(null))
    whenever(settingsRepository.faceToFaceHandicapFlow).thenReturn(flowOf(null))

    viewModel = FaceToFaceViewModel(
      analytics = analytics,
      crashlytics = crashlytics,
      settingsRepository = settingsRepository,
      sessionEngine = sessionEngine,
      estimator = estimator,
      lanConnectionManager = lanConnectionManager,
      lanDiscoveryManager = lanDiscoveryManager,
      applicationScope = applicationTestScope,
      testing = true
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    applicationTestScope.cancel()
  }

  @Test
  fun `smoke test`() = runTest {
    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3)))

      var item = awaitState { it.history.size == 1 }
      assertEquals(1, item.position?.blackStones?.size)
      assertEquals(0, item.position?.whiteStones?.size)
      assertEquals(1, item.history.size)
      assertEquals(StoneType.WHITE, item.position?.nextToMove)
      assertEquals(6.5f, item.position?.komi)

      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2)))

      item = awaitState { it.history.size == 2 }
      assertEquals(1, item.position?.blackStones?.size)
      assertEquals(1, item.position?.whiteStones?.size)
      assertEquals(2, item.history.size)
      assertEquals(StoneType.BLACK, item.position?.nextToMove)

      viewModel.onAction(Action.BoardCellTapUp(Cell(2, 2)))

      item = awaitState { it.history.size == 3 }
      assertEquals(2, item.position?.blackStones?.size)
      assertEquals(1, item.position?.whiteStones?.size)
      assertEquals(3, item.history.size)
      assertEquals(StoneType.WHITE, item.position?.nextToMove)

      viewModel.onAction(Action.BoardCellTapUp(Cell(2, 3)))
      awaitState { it.history.size == 4 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(4, 2)))
      awaitState { it.history.size == 5 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 4)))
      awaitState { it.history.size == 6 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 1)))
      awaitState { it.history.size == 7 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(4, 3)))
      awaitState { it.history.size == 8 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(5, 2)))
      awaitState { it.history.size == 9 }
      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 2)))

      item = awaitState { it.history.size == 10 }
      assertEquals(4, item.position?.blackStones?.size)
      assertEquals(4, item.position?.whiteStones?.size)
      assertEquals(1, item.position?.whiteCaptureCount)
      assertEquals(1, item.position?.blackCaptureCount)
      assertEquals(10, item.history.size)
      assertEquals(StoneType.BLACK, item.position?.nextToMove)

      viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3)))
      item = awaitState { it.koMoveDialogShowing }
      assertEquals(4, item.position?.blackStones?.size)
      assertEquals(4, item.position?.whiteStones?.size)
      assertEquals(1, item.position?.whiteCaptureCount)
      assertEquals(1, item.position?.blackCaptureCount)
      assertEquals(10, item.history.size)
      assertEquals(StoneType.BLACK, item.position?.nextToMove)
      assertTrue(item.koMoveDialogShowing)

      viewModel.onAction(Action.KOMoveDialogDismiss)
      item = awaitState { !it.koMoveDialogShowing }
      assertFalse(item.koMoveDialogShowing)

      cancel()
    }
  }

  @Test
  fun `ko is recognized`() = runTest {
    val moves = "E5, D6, E7, E8, E6, F6, F7, D7, G6, F8, F5, F4, G5, H5, E4, H6, G4, H4, F3, D5, E3, D3, D4, C4, C6, E2, C5, F2, C7, C8, D8, D9, D7, B7, B8, B6, C9, B9, E9, F9, G8, H8, G9, H9, H7, J7, J8, J9, J8, J6, A9, B5, B4, A3, A4, B3, A5, A6, A7, A8, A7"
      .split(", ")
      .map { Cell.fromGTP(it, 9) }

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      viewModel.onAction(Action.NewGameParametersChanged(GameParameters(BoardSize.SMALL, 0)))
      awaitState { it.newGameParameters == GameParameters(BoardSize.SMALL, 0) }
      viewModel.onAction(Action.StartNewGame)
      awaitState {
        !it.loading &&
          it.history.isEmpty() &&
          it.currentGameParameters == GameParameters(BoardSize.SMALL, 0)
      }
      moves.dropLast(1).forEachIndexed { index, cell ->
        viewModel.onAction(Action.BoardCellTapUp(cell))
        awaitState { it.history.size == index + 1 }
      }
      viewModel.onAction(Action.BoardCellTapUp(moves.last()))
      val item = awaitState { it.koMoveDialogShowing }
      assertTrue(item.koMoveDialogShowing)
      assertEquals(moves.dropLast(1), item.history)
      cancel()
    }
  }

  @Test
  fun `peer turn state stays consistent after start remote move and local move`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }

      transport.emitIncoming(startGameMessage())
      var item = awaitState {
        it.title == "Face to face · Opponent's turn" &&
          it.extraStatus == "Opponent's turn." &&
          it.history.isEmpty()
      }
      assertFalse(item.boardInteractive)

      transport.emitIncoming(
        FaceToFacePeerMessage.Move(
          sessionId = SESSION_ID,
          moveNumber = 1,
          player = StoneType.BLACK,
          cell = Cell(3, 3),
        )
      )
      item = awaitState { it.history.size == 1 }
      assertEquals("Face to face · Your turn", item.title)
      assertEquals("Your turn.", item.extraStatus)
      assertTrue(item.boardInteractive)

      viewModel.onAction(Action.BoardCellTapUp(Cell(15, 15)))
      item = awaitState { it.history.size == 2 }
      assertEquals("Face to face · Opponent's turn", item.title)
      assertEquals("Opponent's turn.", item.extraStatus)
      assertFalse(item.boardInteractive)

      val sentMove = transport.sentMessages.filterIsInstance<FaceToFacePeerMessage.Move>().last()
      assertEquals(2, sentMove.moveNumber)
      assertEquals(StoneType.WHITE, sentMove.player)
      assertEquals(Cell(15, 15), sentMove.cell)
      cancel()
    }
  }

  @Test
  fun `guest repeated pass opens estimation without corrupting move history`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin(size = BoardSize.SMALL)
      awaitState { it.title == "Face to face · Connecting" }
      transport.emitIncoming(startGameMessage(boardSize = 9))
      awaitState { it.title == "Face to face · Opponent's turn" }

      transport.emitIncoming(
        FaceToFacePeerMessage.Move(
          sessionId = SESSION_ID,
          moveNumber = 1,
          player = StoneType.BLACK,
          cell = PASS,
        )
      )
      awaitState {
        it.history.size == 1 &&
          it.title == "Face to face · Your turn" &&
          !it.drawTerritory
      }

      viewModel.onAction(Action.BottomButtonPressed(Button.Pass()))
      applicationTestScope.advanceUntilIdle()
      assertEquals(listOf(PASS, PASS), currentSession().moveHistory.takeLast(2))
      assertTrue(currentEstimateStatus() is EstimateStatus.Success)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `host disconnect shows resume message`() = runTest {
    val transport = FakeTransport()
    stubHostTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }
      cancelAndIgnoreRemainingEvents()
    }

    startWifiHost()
    applicationTestScope.advanceUntilIdle()
      assertEquals(FaceToFaceConnectionState.CONNECTED, currentSession().connectionState)
      assertTrue(currentSession().isLocalTurn)

    transport.disconnect(null)
    applicationTestScope.advanceUntilIdle()
    assertEquals(FaceToFaceConnectionState.DISCONNECTED, currentSession().connectionState)
    assertEquals("Guest disconnected. Start hosting again to resume this game.", currentSetupMessage())
  }

  @Test
  fun `guest disconnect shows resume message`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      transport.emitIncoming(startGameMessage())
      awaitState { it.title == "Face to face · Opponent's turn" }

      transport.disconnect(null)
      val item = awaitState { it.title == "Face to face · Disconnected" }
      assertEquals("Disconnected from host. Reconnect when the host is ready to resume.", item.extraStatus)
      assertFalse(item.boardInteractive)
      cancel()
    }
  }

  @Test
  fun `guest disconnect keeps current board visible while waiting to resume`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      transport.emitIncoming(
        startGameMessage(
          moves = listOf(Cell(3, 3), Cell(15, 15))
        )
      )
      awaitState {
        it.title == "Face to face · Opponent's turn" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }

      transport.disconnect(null)
      val disconnected = awaitState {
        it.title == "Face to face · Disconnected" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }
      assertEquals("Disconnected from host. Reconnect when the host is ready to resume.", disconnected.extraStatus)
      assertFalse(disconnected.boardInteractive)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onCleared closes active peer transport`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      assertEquals(0, transport.closeCalls)

      invokeOnCleared()
      applicationTestScope.advanceUntilIdle()

      assertEquals(1, transport.closeCalls)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `guest disconnects on incompatible host hello protocol`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }

      transport.emitIncoming(
        FaceToFacePeerMessage.Hello(
          sessionId = SESSION_ID,
          protocolVersion = 999,
          deviceName = "Host device",
        )
      )
      val item = awaitState { it.title == "Face to face · Disconnected" }
      assertEquals("Incompatible face-to-face version on the other device", item.extraStatus)
      assertTrue(item.newGameDialogShowing)
      assertFalse(item.boardInteractive)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `host manual resume reuses prior snapshot`() = runTest {
    val firstTransport = FakeTransport()
    val secondTransport = FakeTransport()
    stubHostTransports(firstTransport, secondTransport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }
      cancelAndIgnoreRemainingEvents()
    }

    startWifiHost()
    applicationTestScope.advanceUntilIdle()
    val hostSessionId = currentSession().sessionId

    viewModel.onAction(Action.BoardCellTapUp(Cell(3, 3)))
    applicationTestScope.advanceUntilIdle()
    assertEquals(listOf(Cell(3, 3)), currentSession().moveHistory)
    assertFalse(currentSession().isLocalTurn)

    firstTransport.emitIncoming(
      FaceToFacePeerMessage.Move(
        sessionId = hostSessionId,
        moveNumber = 2,
        player = StoneType.WHITE,
        cell = Cell(15, 15),
      )
    )
    applicationTestScope.advanceUntilIdle()
    assertEquals(listOf(Cell(3, 3), Cell(15, 15)), currentSession().moveHistory)
    assertTrue(currentSession().isLocalTurn)

    firstTransport.disconnect(null)
    applicationTestScope.advanceUntilIdle()
    assertEquals(FaceToFaceConnectionState.DISCONNECTED, currentSession().connectionState)

    startWifiHost()
    applicationTestScope.advanceUntilIdle()
    val resumedSession = currentSession()
    assertEquals(FaceToFaceConnectionState.CONNECTED, resumedSession.connectionState)
    assertEquals(listOf(Cell(3, 3), Cell(15, 15)), resumedSession.moveHistory)
    assertTrue(resumedSession.isLocalTurn)

    val replayedSnapshot = secondTransport.sentMessages
      .filterIsInstance<FaceToFacePeerMessage.StartGame>()
      .single()
      .snapshot
    assertEquals(listOf(Cell(3, 3), Cell(15, 15)), replayedSnapshot.moveHistory)
  }

  @Test
  fun `guest ignores sync state from different session`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      transport.emitIncoming(startGameMessage())
      awaitState { it.title == "Face to face · Opponent's turn" }

      transport.emitIncoming(
        FaceToFacePeerMessage.SyncState(
          sessionId = "other-session",
          snapshot = FaceToFaceGameSnapshot(
            sessionId = "other-session",
            config = FaceToFaceGameConfig(boardSize = 19, handicap = 0),
            moveHistory = listOf(Cell(3, 3), Cell(15, 15)),
          ),
        )
      )
      applicationTestScope.advanceUntilIdle()
      assertEquals(emptyList<Cell>(), currentSession().moveHistory)
      assertEquals(FaceToFaceConnectionState.CONNECTED, currentSession().connectionState)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `guest manual resume keeps board until host snapshot arrives`() = runTest {
    val firstTransport = FakeTransport()
    val secondTransport = FakeTransport()
    stubJoinTransports(firstTransport, secondTransport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      firstTransport.emitIncoming(
        startGameMessage(
          moves = listOf(Cell(3, 3), Cell(15, 15))
        )
      )
      awaitState {
        it.title == "Face to face · Opponent's turn" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }

      firstTransport.disconnect(null)
      awaitState { it.title == "Face to face · Disconnected" }

      startWifiJoin()
      val reconnecting = awaitState {
        it.title == "Face to face · Connecting" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }
      assertTrue(reconnecting.extraStatus?.isNotBlank() == true)

      secondTransport.emitIncoming(
        startGameMessage(
          moves = listOf(Cell(3, 3), Cell(15, 15))
        )
      )
      val resumed = awaitState {
        it.title == "Face to face · Opponent's turn" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }
      assertEquals("Opponent's turn.", resumed.extraStatus)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `out of sync recovery requests snapshot then restores connected state`() = runTest {
    val transport = FakeTransport()
    stubJoinTransports(transport)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.molecule()
    }.test {
      awaitState { !it.loading }

      startWifiJoin()
      awaitState { it.title == "Face to face · Connecting" }
      transport.emitIncoming(startGameMessage())
      awaitState { it.title == "Face to face · Opponent's turn" }

      transport.emitIncoming(
        FaceToFacePeerMessage.Move(
          sessionId = SESSION_ID,
          moveNumber = 3,
          player = StoneType.BLACK,
          cell = Cell(3, 3),
        )
      )
      val syncing = awaitState { it.title == "Face to face · Syncing" }
      assertEquals("Move mismatch detected. Syncing board...", syncing.extraStatus)
      assertFalse(syncing.boardInteractive)

      val syncRequest = transport.sentMessages.filterIsInstance<FaceToFacePeerMessage.SyncRequest>().single()
      assertEquals(0, syncRequest.expectedMoveCount)

      transport.emitIncoming(
        FaceToFacePeerMessage.SyncState(
          sessionId = SESSION_ID,
          snapshot = FaceToFaceGameSnapshot(
            sessionId = SESSION_ID,
            config = FaceToFaceGameConfig(boardSize = 19, handicap = 0),
            moveHistory = listOf(Cell(3, 3), Cell(15, 15)),
          ),
        )
      )
      val restored = awaitState {
        it.title == "Face to face · Opponent's turn" &&
          it.history == listOf(Cell(3, 3), Cell(15, 15))
      }
      assertEquals("Opponent's turn.", restored.extraStatus)
      assertFalse(restored.boardInteractive)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun stubJoinTransports(vararg transports: FakeTransport) {
    lanConnectionManager.enqueueJoinTransports(*transports)
  }

  private fun stubHostTransports(vararg transports: FakeTransport) {
    lanConnectionManager.enqueueHostTransports(*transports)
  }

  private fun startWifiJoin(
    size: BoardSize = BoardSize.LARGE,
    handicap: Int = 0,
    hostAddress: String = "192.168.0.10",
  ) {
    viewModel.onAction(
      Action.NewGameParametersChanged(
        GameParameters(
          size = size,
          handicap = handicap,
          mode = MatchMode.WIFI_JOIN,
          hostAddress = hostAddress,
        )
      )
    )
    viewModel.onAction(Action.StartNewGame)
  }

  private fun startWifiHost(
    size: BoardSize = BoardSize.LARGE,
    handicap: Int = 0,
  ) {
    viewModel.onAction(
      Action.NewGameParametersChanged(
        GameParameters(
          size = size,
          handicap = handicap,
          mode = MatchMode.WIFI_HOST,
        )
      )
    )
    viewModel.onAction(Action.StartNewGame)
  }

  private fun startGameMessage(
    boardSize: Int = 19,
    handicap: Int = 0,
    moves: List<Cell> = emptyList(),
  ): FaceToFacePeerMessage.StartGame {
    return FaceToFacePeerMessage.StartGame(
      sessionId = SESSION_ID,
      snapshot = FaceToFaceGameSnapshot(
        sessionId = SESSION_ID,
        config = FaceToFaceGameConfig(boardSize = boardSize, handicap = handicap),
        moveHistory = moves,
      ),
    )
  }

  private suspend fun ReceiveTurbine<FaceToFaceState>.awaitState(
    predicate: (FaceToFaceState) -> Boolean
  ): FaceToFaceState {
    while (true) {
      val item = awaitItem()
      if (predicate(item)) {
        return item
      }
    }
  }

  private fun currentSession(): FaceToFaceSessionState {
    val method = FaceToFaceViewModel::class.java.getDeclaredMethod("getSession")
    method.isAccessible = true
    return method.invoke(viewModel) as FaceToFaceSessionState
  }

  private fun currentSetupMessage(): String? {
    val method = FaceToFaceViewModel::class.java.getDeclaredMethod("getSetupMessage")
    method.isAccessible = true
    return method.invoke(viewModel) as String?
  }

  private fun currentEstimateStatus(): EstimateStatus {
    val method = FaceToFaceViewModel::class.java.getDeclaredMethod("getEstimateStatus")
    method.isAccessible = true
    return method.invoke(viewModel) as EstimateStatus
  }

  private fun invokeOnCleared() {
    val method = FaceToFaceViewModel::class.java.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(viewModel)
  }

  private inner class FakeTransport : FaceToFaceTransport {
    private val incoming = MutableSharedFlow<FaceToFacePeerMessage>(extraBufferCapacity = 16)
    private var onClosed: ((Throwable?) -> Unit)? = null
    val sentMessages = mutableListOf<FaceToFacePeerMessage>()
    var closeCalls = 0
      private set

    override val incomingMessages = incoming.asSharedFlow()

    override suspend fun send(message: FaceToFacePeerMessage) {
      sentMessages += message
    }

    override suspend fun close() {
      closeCalls += 1
      onClosed?.invoke(null)
    }

    suspend fun emitIncoming(message: FaceToFacePeerMessage) {
      incoming.emit(message)
    }

    fun bindOnClosed(callback: (Throwable?) -> Unit) {
      onClosed = callback
    }

    fun disconnect(error: Throwable?) {
      onClosed?.invoke(error)
    }
  }

  private inner class FakeLanConnectionManager : FaceToFacePeerConnectionManager {
    private val joinTransports = ArrayDeque<FakeTransport>()
    private val hostTransports = ArrayDeque<FakeTransport>()

    fun enqueueJoinTransports(vararg transports: FakeTransport) {
      joinTransports.addAll(transports)
    }

    fun enqueueHostTransports(vararg transports: FakeTransport) {
      hostTransports.addAll(transports)
    }

    override suspend fun host(
      port: Int,
      onClosed: (Throwable?) -> Unit,
    ): FaceToFaceLanHostHandle {
      check(hostTransports.isNotEmpty()) { "No fake host transport left" }
      val transport = hostTransports.removeFirst()
      transport.bindOnClosed(onClosed)
      return FaceToFaceLanHostHandle(
        localAddress = "192.168.0.10",
        port = FaceToFaceLanConnectionManager.DEFAULT_PORT,
        awaitTransport = { transport },
        closeServer = {},
      )
    }

    override suspend fun join(
      hostAddress: String,
      port: Int,
      onClosed: (Throwable?) -> Unit,
    ): FaceToFaceTransport {
      check(joinTransports.isNotEmpty()) { "No fake join transport left" }
      return joinTransports.removeFirst().also { it.bindOnClosed(onClosed) }
    }
  }

  private class FakeFaceToFaceEstimator : FaceToFaceEstimator {
    override suspend fun determineTerritory(position: Position): Position = position
  }

  private companion object {
    val PASS = Cell(-1, -1)
    const val SESSION_ID = "test-peer-session"
  }
}
