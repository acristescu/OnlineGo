package io.zenandroid.onlinego.ui.screens.face2face

import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType.BLACK
import io.zenandroid.onlinego.data.model.StoneType.WHITE
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.composables.BottomBarButton
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.face2face.Action.BottomButtonPressed
import io.zenandroid.onlinego.ui.screens.face2face.Action.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NewGameDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NewGameParametersChanged
import io.zenandroid.onlinego.ui.screens.face2face.Button.CloseEstimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.Estimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.GameSettings
import io.zenandroid.onlinego.ui.screens.face2face.Button.Next
import io.zenandroid.onlinego.ui.screens.face2face.Button.Pass
import io.zenandroid.onlinego.ui.screens.face2face.Button.Previous
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Idle
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Success
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Working
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceGameConfig
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceGameSnapshot
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanConnectionManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanDiscoveredHost
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanDiscoveryManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanHostHandle
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanJoinTarget
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFacePeerConnectionManager
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFacePeerMessage
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFacePeerRole
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceMoveRejectReason
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionMode
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionEngine
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionMutationResult
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSessionState
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceTransport
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceTransportType
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceSyncRecoveryAction
import io.zenandroid.onlinego.ui.screens.face2face.session.FACE_TO_FACE_PROTOCOL_VERSION
import io.zenandroid.onlinego.ui.screens.face2face.session.buildFaceToFaceLanJoinErrorMessage
import io.zenandroid.onlinego.ui.screens.face2face.session.buildFaceToFaceLanHostErrorMessage
import io.zenandroid.onlinego.ui.screens.face2face.session.parseFaceToFaceLanJoinTarget
import io.zenandroid.onlinego.ui.screens.face2face.session.resolveOutOfSyncRecovery
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FaceToFaceViewModel(
  private val analytics: FirebaseAnalytics,
  private val crashlytics: FirebaseCrashlytics,
  private val settingsRepository: SettingsRepository,
  private val sessionEngine: FaceToFaceSessionEngine,
  private val estimator: FaceToFaceEstimator,
  private val lanConnectionManager: FaceToFacePeerConnectionManager,
  private val lanDiscoveryManager: FaceToFaceLanDiscoveryManager,
  private val applicationScope: CoroutineScope,
  testing: Boolean = false
) : ViewModel() {

  private val moleculeScope =
    if (testing) viewModelScope else CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
  private val workerDispatcher: CoroutineDispatcher =
    if (testing) Dispatchers.Main else Dispatchers.Default
  private val ioDispatcher: CoroutineDispatcher =
    if (testing) Dispatchers.Main else Dispatchers.IO

  private var loading by mutableStateOf(true)
  private var session by mutableStateOf<FaceToFaceSessionState?>(null)
  private var transport: FaceToFaceTransport? = null
  private var hostHandle: FaceToFaceLanHostHandle? = null
  private var transportMessagesJob: Job? = null
  private var discoveryJob: Job? = null
  private var candidateMove by mutableStateOf<Cell?>(null)
  private var historyIndex by mutableStateOf<Int?>(null)
  private var koMoveDialogShowing by mutableStateOf(false)
  private var estimateStatus by mutableStateOf<EstimateStatus>(Idle)
  private var newGameDialogShowing by mutableStateOf(false)
  private var setupMessage by mutableStateOf<String?>(null)
  private var discoveredHosts by mutableStateOf<List<FaceToFaceLanDiscoveredHost>>(emptyList())
  private var currentGameParameters by mutableStateOf(GameParameters(BoardSize.LARGE, 0))
  private var newGameParameters by mutableStateOf(GameParameters(BoardSize.LARGE, 0))
  @Volatile
  private var suppressPeerCloseCallback = false

  init {
    analytics.logEvent("face_to_face_opened", null)
    viewModelScope.launch(ioDispatcher) {
      loadSavedData()
    }
  }

  val state: StateFlow<FaceToFaceState> =
    if (testing) MutableStateFlow(FaceToFaceState.INITIAL)
    else moleculeScope.launchMolecule(mode = ContextClock) {
      molecule()
    }

  @VisibleForTesting
  @Composable
  fun molecule(): FaceToFaceState {
    val session = session
    val history = session?.moveHistory ?: emptyList()
    val historyIndex = historyIndex
    val activePosition = displayedPosition(session)
    val peerSessionActive = session?.mode == FaceToFaceSessionMode.PEER_TO_PEER
    val sessionStatus = sessionStatus(session)

    val title = when {
      loading -> "Face to face · Loading"
      session?.mode == FaceToFaceSessionMode.PEER_TO_PEER -> peerSessionTitle(session)
      activePosition.nextToMove == WHITE -> "Face to face · White's turn"
      activePosition.nextToMove == BLACK -> "Face to face · Black's turn"
      else -> "Face to face"
    }

    val estimateStatus = estimateStatus
    val position = when {
      estimateStatus is Success -> estimateStatus.result
      else -> activePosition
    }

    val boardInteractive =
      !loading &&
        estimateStatus is Idle &&
        when {
          session == null -> false
          session.mode == FaceToFaceSessionMode.HOTSEAT -> true
          session.connectionState != io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED -> false
          historyIndex != null -> false
          else -> session.isLocalTurn
        }
    val previousButtonEnabled =
      !peerSessionActive && !loading && history.isNotEmpty() && (historyIndex == null || historyIndex >= 0)
    val nextButtonEnabled =
      !peerSessionActive && !loading && history.isNotEmpty() && historyIndex != null && historyIndex < history.size

    val (buttons, bottomText) = when {
      estimateStatus is Working -> emptyList<Button>() to "Estimating"
      estimateStatus is Success -> listOf(CloseEstimate) to null
      peerSessionActive -> listOf(GameSettings, Estimate, Pass(boardInteractive)) to null
      else -> listOf(
        GameSettings, Estimate, Pass(true), Previous(previousButtonEnabled), Next(nextButtonEnabled)
      ) to null
    }

    val extraStatus = when {
      estimateStatus is Success && estimateStatus.gameIsOver -> "Game is over!"
      estimateStatus is Success && !estimateStatus.gameIsOver -> "Recommendation: Game is not over!"
      sessionStatus != null -> sessionStatus
      else -> null
    }

    return FaceToFaceState(
      loading = loading,
      position = position,
      title = title,
      gameFinished = false,
      history = history,
      boardInteractive = boardInteractive,
      candidateMove = candidateMove,
      drawTerritory = estimateStatus is Success,
      fadeOutRemovedStones = estimateStatus is Success,
      showLastMove = estimateStatus !is Success,
      koMoveDialogShowing = koMoveDialogShowing,
      buttons = buttons,
      bottomText = bottomText,
      newGameDialogShowing = newGameDialogShowing,
      currentGameParameters = currentGameParameters,
      newGameParameters = newGameParameters,
      blackPlayerLabel = blackPlayerLabel(session),
      whitePlayerLabel = whitePlayerLabel(session),
      setupMessage = setupMessage,
      discoveredHosts = discoveredHosts,
      extraStatus = extraStatus,
    )
  }

  private suspend fun loadSavedData() {
    val historyString = settingsRepository.faceToFaceHistoryFlow.first() ?: ""
    val sizeString =
      settingsRepository.faceToFaceBoardSizeFlow.first() ?: BoardSize.LARGE.prettyName
    val handicap = settingsRepository.faceToFaceHandicapFlow.first() ?: 0
    val size = BoardSize.entries.firstOrNull { it.prettyName == sizeString } ?: BoardSize.LARGE
    val history = historyString.split(" ")
      .filter { it.isNotEmpty() }
      .map {
        val parts = it.split(",")
        Cell(parts[0].toInt(), parts[1].toInt())
      }
    val params = GameParameters(size, handicap, mode = MatchMode.HOTSEAT)

    currentGameParameters = params
    newGameParameters = params
    setupMessage = null

    session = try {
      if (history.isNotEmpty()) {
        analytics.logEvent("face_to_face_loading", null)
      }
      val snapshot = FaceToFaceGameSnapshot(
        sessionId = HOTSEAT_SESSION_ID,
        config = params.toSessionConfig(),
        moveHistory = history,
      )
      sessionEngine.restoreFromSnapshot(snapshot)
    } catch (e: Exception) {
      crashlytics.log("FaceToFaceViewModel Cannot load saved hotseat session")
      safeRecordException(e)
      sessionEngine.createHotseatSession(
        config = params.toSessionConfig(),
        sessionId = HOTSEAT_SESSION_ID,
      )
    }
    loading = false
    analytics.logEvent("face_to_face_loaded", null)
  }

  override fun onCleared() {
    val session = session
    applicationScope.launch {
      if (session?.mode == FaceToFaceSessionMode.HOTSEAT) {
        settingsRepository.setFaceToFaceHistory(
          session.moveHistory.joinToString(separator = " ") { "${it.x},${it.y}" }
        )
        settingsRepository.setFaceToFaceBoardSize(currentGameParameters.size.toString())
        settingsRepository.setFaceToFaceHandicap(currentGameParameters.handicap)
      }
    }
    applicationScope.launch(ioDispatcher) {
      closePeerConnection()
    }
    stopJoinDiscovery(clearHosts = true)
    super.onCleared()
  }

  fun onAction(action: Action) {
    when (action) {
      is BoardCellDragged -> candidateMove = action.cell
      is BoardCellTapUp -> onCellTapUp(action.cell)
      KOMoveDialogDismiss -> koMoveDialogShowing = false
      is BottomButtonPressed -> onButtonPressed(action.button)
      NewGameDialogDismiss -> {
        newGameDialogShowing = false
        updateJoinDiscovery()
      }
      is NewGameParametersChanged -> {
        newGameParameters = action.params
        updateJoinDiscovery()
      }
      Action.StartNewGame -> onStartNewGame()
    }
  }

  private fun onButtonPressed(button: Button) {
    when (button) {
      is Estimate -> doEstimation()
      is GameSettings -> {
        newGameDialogShowing = true
        updateJoinDiscovery()
      }
      is Next -> onNextPressed()
      is Previous -> onPreviousPressed()
      is CloseEstimate -> estimateStatus = Idle
      is Pass -> onPassPressed()
    }
  }

  private fun doEstimation() {
    val position = displayedPosition()
    estimateStatus = Working
    viewModelScope.launch(ioDispatcher) {
      val estimate = estimator.determineTerritory(position)
      withContext(Dispatchers.Main) {
        val history = session?.moveHistory.orEmpty()
        val index = historyIndex ?: history.lastIndex
        val finished =
          index > currentGameParameters.size.width &&
              estimate.dame.size < currentGameParameters.size.width
        estimateStatus = Success(estimate, finished)
      }
    }
  }

  private fun onPreviousPressed() {
    val history = session?.moveHistory ?: return
    crashlytics.log("FaceToFaceViewModel onPreviousPressed")
    val newIndex = historyIndex?.minus(1) ?: (history.lastIndex - 1)
    if (newIndex < -1) {
      return
    }
    historyIndex = newIndex
  }

  private fun onNextPressed() {
    val history = session?.moveHistory ?: return
    crashlytics.log("FaceToFaceViewModel onNextPressed")
    val newIndex = historyIndex?.plus(1) ?: history.lastIndex
    if (newIndex > history.lastIndex) {
      return
    }
    historyIndex = if (newIndex < history.lastIndex) newIndex else null
  }

  private fun onStartNewGame() {
    crashlytics.log("FaceToFaceViewModel Starting new game")
    viewModelScope.launch(workerDispatcher) {
      val params = newGameParameters
      runCatching {
        when (params.mode) {
          MatchMode.HOTSEAT -> startHotseatGame(params)
          MatchMode.WIFI_HOST -> startWifiHost(params)
          MatchMode.WIFI_JOIN -> startWifiJoin(params)
        }
      }.onFailure {
        if (params.mode == MatchMode.HOTSEAT) {
          crashlytics.log("Unable to start hotseat game")
          safeRecordException(it)
        } else if (params.mode == MatchMode.WIFI_HOST) {
          handleHostStartFailure(it)
        } else {
          handlePeerFailure("Unable to start ${params.mode}", it, reopenDialog = true)
        }
      }
    }
  }

  private suspend fun startHotseatGame(params: GameParameters) {
    closePeerConnection()
    session = sessionEngine.createHotseatSession(
      config = params.toSessionConfig(),
      sessionId = HOTSEAT_SESSION_ID,
    )
    resetTransientUi()
    setupMessage = null
    currentGameParameters = params.copy(mode = MatchMode.HOTSEAT, hostAddress = "")
    newGameParameters = currentGameParameters
  }

  private suspend fun startWifiHost(params: GameParameters) {
    val reconnectSession = session.takeIf { it.canReconnectPeerSession(params, FaceToFacePeerRole.HOST) }
    closePeerConnection()
    val peerSession = reconnectSession?.copy(
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.HOSTING,
      localPlayerName = localDeviceName(),
      lastError = null,
    ) ?: sessionEngine.createPeerSession(
      config = params.toSessionConfig(),
      localRole = FaceToFacePeerRole.HOST,
      transport = FaceToFaceTransportType.WIFI_LAN,
      sessionId = UUID.randomUUID().toString(),
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.HOSTING,
      localPlayerName = localDeviceName(),
      remotePlayerName = "Guest",
    )
    session = peerSession
    resetTransientUi()
    currentGameParameters = params.copy(mode = MatchMode.WIFI_HOST, hostAddress = "")
    newGameParameters = currentGameParameters
    setupMessage = if (peerSession.moveHistory.isEmpty()) {
      "Preparing local Wi-Fi host..."
    } else {
      "Preparing to resume the Wi-Fi game..."
    }

    val hostHandle = withContext(ioDispatcher) {
      lanConnectionManager.host(onClosed = ::onPeerConnectionClosed)
    }
    this.hostHandle = hostHandle
    val autodiscoveryAvailable = runCatching {
      lanDiscoveryManager.startAdvertising(
        sessionId = peerSession.sessionId,
        deviceName = localDeviceName(),
        port = hostHandle.port,
      )
    }.isSuccess
    setupMessage = hostSetupMessage(
      host = FaceToFaceLanJoinTarget(hostHandle.localAddress, hostHandle.port),
      reconnecting = peerSession.moveHistory.isNotEmpty(),
    ) + if (autodiscoveryAvailable) "" else " Autodiscovery unavailable."

    val transport = withContext(ioDispatcher) {
      hostHandle.awaitTransport()
    }
    runCatching { lanDiscoveryManager.stopAdvertising() }
    this.hostHandle = null
    attachTransport(transport)
    session = session?.copy(
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED,
      lastError = null,
    )
    setupMessage = null

    val connectedSession = session ?: return
    runCatching {
      transport.send(
        FaceToFacePeerMessage.Hello(
          sessionId = connectedSession.sessionId,
          deviceName = localDeviceName(),
        )
      )
      transport.send(
        FaceToFacePeerMessage.StartGame(
          sessionId = connectedSession.sessionId,
          snapshot = sessionEngine.toSnapshot(connectedSession),
        )
      )
    }.onFailure { handlePeerFailure("Unable to start Wi-Fi game", it) }
  }

  private suspend fun startWifiJoin(params: GameParameters) {
    val hostInput = params.hostAddress.trim()
    val joinTarget = runCatching {
      parseFaceToFaceLanJoinTarget(hostInput)
    }.getOrElse {
      setupMessage = it.message ?: "Enter the host address."
      newGameDialogShowing = true
      return
    }

    val reconnectSession = session.takeIf { it.canReconnectPeerSession(params, FaceToFacePeerRole.GUEST) }
    closePeerConnection()
    session = reconnectSession?.copy(
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTING,
      localPlayerName = localDeviceName(),
      lastError = null,
    ) ?: sessionEngine.createPeerSession(
      config = params.toSessionConfig(),
      localRole = FaceToFacePeerRole.GUEST,
      transport = FaceToFaceTransportType.WIFI_LAN,
      sessionId = PENDING_SESSION_ID,
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTING,
      localPlayerName = localDeviceName(),
      remotePlayerName = "Host",
    )
    resetTransientUi()
    currentGameParameters = params.copy(mode = MatchMode.WIFI_JOIN, hostAddress = hostInput)
    newGameParameters = currentGameParameters
    setupMessage = "Connecting to ${joinTarget.host}:${joinTarget.port}..."

    val transport = try {
      withContext(ioDispatcher) {
        lanConnectionManager.join(joinTarget.host, port = joinTarget.port, onClosed = ::onPeerConnectionClosed)
      }
    } catch (e: Exception) {
      handleJoinConnectionFailure(joinTarget, e)
      return
    }

    attachTransport(transport)
    stopJoinDiscovery(clearHosts = false)
    setupMessage = "Connected. Waiting for the host to start the game."
    runCatching {
      transport.send(
        FaceToFacePeerMessage.Hello(
          sessionId = session?.sessionId ?: PENDING_SESSION_ID,
          deviceName = localDeviceName(),
        )
      )
    }.onFailure { handlePeerFailure("Unable to introduce this device to the host", it, reopenDialog = true) }
  }

  private fun onPassPressed() {
    onCellTapUp(Cell(-1, -1))
  }

  private fun onCellTapUp(cell: Cell) {
    viewModelScope.launch(workerDispatcher) {
      val session = session ?: return@launch
      val baseSession = if (session.mode == FaceToFaceSessionMode.HOTSEAT) {
        rewindSession(session, historyIndex)
      } else {
        session
      }
      when (val result = sessionEngine.applyLocalMove(baseSession, cell)) {
        is FaceToFaceSessionMutationResult.Applied -> {
          this@FaceToFaceViewModel.session = result.state
          if (result.state.mode == FaceToFaceSessionMode.PEER_TO_PEER) {
            setupMessage = null
          }
          val repeatedPass = cell.isPass && baseSession.moveHistory.lastOrNull()?.isPass == true
          historyIndex = null
          if (result.state.mode == FaceToFaceSessionMode.PEER_TO_PEER) {
            val player = baseSession.position.nextToMove
            runCatching {
              transport?.send(
                FaceToFacePeerMessage.Move(
                  sessionId = result.state.sessionId,
                  moveNumber = result.state.moveHistory.size,
                  player = player,
                  cell = cell,
                )
              )
            }.onFailure { handlePeerFailure("Unable to send move to the other device", it) }
          }
          if (repeatedPass) {
            doEstimation()
          }
        }

        is FaceToFaceSessionMutationResult.Rejected -> if (result.reason == FaceToFaceMoveRejectReason.KO) {
          crashlytics.log("FaceToFaceViewModel KO move detected")
          koMoveDialogShowing = true
        }
      }
      candidateMove = null
    }
  }

  private fun displayedPosition(
    session: FaceToFaceSessionState? = this.session,
  ): Position {
    if (session == null) {
      return RulesManager.initializePosition(
        currentGameParameters.size.width,
        currentGameParameters.handicap,
      )
    }

    val historyIndex = historyIndex
    return when {
      historyIndex == null -> session.position
      historyIndex < 0 -> session.initialPosition
      else -> session.positionHistory.getOrNull(historyIndex) ?: session.position
    }
  }

  private suspend fun rewindSession(
    session: FaceToFaceSessionState,
    historyIndex: Int?,
  ): FaceToFaceSessionState {
    if (historyIndex == null) return session
    return sessionEngine.rewindToMoveCount(session, historyIndex + 1)
  }

  private suspend fun attachTransport(transport: FaceToFaceTransport) {
    this.transport = transport
    transportMessagesJob?.cancel()
    transportMessagesJob = viewModelScope.launch {
      transport.incomingMessages.collect { message ->
        handleIncomingPeerMessage(message)
      }
    }
  }

  private suspend fun handleIncomingPeerMessage(message: FaceToFacePeerMessage) {
    when (message) {
      is FaceToFacePeerMessage.Hello -> {
        val session = session ?: return
        if (session.mode != FaceToFaceSessionMode.PEER_TO_PEER) return
        if (!session.canAcceptHandshakeSession(message.sessionId)) return
        if (message.protocolVersion != FACE_TO_FACE_PROTOCOL_VERSION) {
          handlePeerFailure(
            message = "Incompatible face-to-face version on the other device",
            error = IllegalStateException(
              "Unsupported protocol version ${message.protocolVersion}"
            ),
            reopenDialog = true,
            appendErrorDetails = false,
          )
          return
        }
        this.session = session.copy(remotePlayerName = message.deviceName)
      }

      is FaceToFacePeerMessage.StartGame -> {
        val currentSession = session ?: return
        if (currentSession.mode != FaceToFaceSessionMode.PEER_TO_PEER) return
        if (currentSession.localRole != FaceToFacePeerRole.GUEST) return
        if (!currentSession.canAcceptHandshakeSession(message.sessionId)) return
        if (message.snapshot.protocolVersion != FACE_TO_FACE_PROTOCOL_VERSION) {
          handlePeerFailure(
            message = "Incompatible face-to-face version on the host device",
            error = IllegalStateException(
              "Unsupported snapshot protocol version ${message.snapshot.protocolVersion}"
            ),
            reopenDialog = true,
            appendErrorDetails = false,
          )
          return
        }
        val restoredSession = sessionEngine.restoreFromSnapshot(
          snapshot = message.snapshot,
          mode = FaceToFaceSessionMode.PEER_TO_PEER,
          localRole = currentSession.localRole,
          transport = FaceToFaceTransportType.WIFI_LAN,
          connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED,
          localPlayerName = localDeviceName(),
          remotePlayerName = currentSession.remotePlayerName ?: "Opponent",
        )
        session = restoredSession
        historyIndex = null
        estimateStatus = Idle
        currentGameParameters = GameParameters(
          size = BoardSize.fromWidth(message.snapshot.config.boardSize),
          handicap = message.snapshot.config.handicap,
          mode = MatchMode.WIFI_JOIN,
          hostAddress = currentGameParameters.hostAddress,
        )
        newGameParameters = currentGameParameters
        setupMessage = null
      }

      is FaceToFacePeerMessage.Move -> {
        val currentSession = session ?: return
        val result = sessionEngine.applyRemoteMove(currentSession, message)
        when (result) {
          is FaceToFaceSessionMutationResult.Applied -> {
            session = result.state
            historyIndex = null
            setupMessage = null
            if (result.state.moveHistory.size >= 2 && result.state.moveHistory.takeLast(2).all(Cell::isPass)) {
              doEstimation()
            }
          }

          is FaceToFaceSessionMutationResult.Rejected -> if (result.reason == FaceToFaceMoveRejectReason.OUT_OF_SYNC) {
            runCatching {
              when (resolveOutOfSyncRecovery(currentSession.moveHistory.size, message.moveNumber)) {
                FaceToFaceSyncRecoveryAction.REQUEST_REMOTE_STATE -> {
                  setupMessage = "Move mismatch detected. Syncing board..."
                  session = currentSession.copy(
                    connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.SYNCING,
                    lastError = null,
                  )
                  transport?.send(
                    FaceToFacePeerMessage.SyncRequest(
                      sessionId = currentSession.sessionId,
                      expectedMoveCount = currentSession.moveHistory.size,
                    )
                  )
                }

                FaceToFaceSyncRecoveryAction.PUSH_LOCAL_STATE -> {
                  session = currentSession.copy(
                    connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED,
                    lastError = null,
                  )
                  setupMessage = "Peer was out of sync. Sending the current board state."
                  transport?.send(
                    FaceToFacePeerMessage.SyncState(
                      sessionId = currentSession.sessionId,
                      snapshot = sessionEngine.toSnapshot(currentSession),
                    )
                  )
                }
              }
            }.onFailure { handlePeerFailure("Unable to sync the board", it) }
          }
        }
      }

      is FaceToFacePeerMessage.SyncRequest -> {
        val currentSession = session ?: return
        if (!currentSession.matchesEstablishedSession(message.sessionId)) return
        if (currentSession.moveHistory.size != message.expectedMoveCount) {
          session = currentSession.copy(
            connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED,
            lastError = null,
          )
          setupMessage = "Sending the current board state to recover sync."
          runCatching {
            transport?.send(
              FaceToFacePeerMessage.SyncState(
                sessionId = currentSession.sessionId,
                snapshot = sessionEngine.toSnapshot(currentSession),
              )
            )
          }.onFailure { handlePeerFailure("Unable to respond to sync request", it) }
        }
      }

      is FaceToFacePeerMessage.SyncState -> {
        val currentSession = session ?: return
        if (!currentSession.matchesEstablishedSession(message.sessionId)) return
        setupMessage = "Syncing board..."
        session = currentSession.copy(
          connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.SYNCING,
          lastError = null,
        )
        session = sessionEngine.restoreFromSnapshot(
          snapshot = message.snapshot,
          mode = currentSession.mode,
          localRole = currentSession.localRole,
          transport = currentSession.transport,
          connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED,
          localPlayerName = currentSession.localPlayerName,
          remotePlayerName = currentSession.remotePlayerName,
        )
        historyIndex = null
        setupMessage = null
      }

      is FaceToFacePeerMessage.KeepAlive,
      is FaceToFacePeerMessage.Resign,
      is FaceToFacePeerMessage.UndoRequest,
      is FaceToFacePeerMessage.UndoResponse -> Unit
    }
  }

  private suspend fun closePeerConnection() {
    suppressPeerCloseCallback = true
    transportMessagesJob?.cancel()
    transportMessagesJob = null
    stopJoinDiscovery(clearHosts = true)

    val hostHandle = hostHandle
    this.hostHandle = null
    val transport = transport
    this.transport = null

    try {
      runCatching { lanDiscoveryManager.stopAdvertising() }
      runCatching { transport?.close() }
      runCatching { hostHandle?.close() }
    } finally {
      suppressPeerCloseCallback = false
    }
  }

  private fun resetTransientUi() {
    estimateStatus = Idle
    koMoveDialogShowing = false
    historyIndex = null
    newGameDialogShowing = false
    stopJoinDiscovery(clearHosts = true)
  }

  private fun blackPlayerLabel(session: FaceToFaceSessionState?): String {
    return when (session?.mode) {
      FaceToFaceSessionMode.PEER_TO_PEER -> "Black"
      else -> "Player 1"
    }
  }

  private fun whitePlayerLabel(session: FaceToFaceSessionState?): String {
    return when (session?.mode) {
      FaceToFaceSessionMode.PEER_TO_PEER -> "White"
      else -> "Player 2"
    }
  }

  private fun peerSessionTitle(session: FaceToFaceSessionState): String {
    return when (session.connectionState) {
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.HOSTING ->
        "Face to face · Waiting for guest"
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTING ->
        "Face to face · Connecting"
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.SYNCING ->
        "Face to face · Syncing"
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.DISCONNECTED ->
        "Face to face · Disconnected"
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED ->
        if (session.isLocalTurn) {
          "Face to face · Your turn"
        } else {
          "Face to face · Opponent's turn"
        }
    }
  }

  private fun peerTurnStatus(session: FaceToFaceSessionState): String {
    return if (session.isLocalTurn) {
      "Your turn."
    } else {
      "Opponent's turn."
    }
  }

  private fun sessionStatus(session: FaceToFaceSessionState?): String? {
    if (session?.mode != FaceToFaceSessionMode.PEER_TO_PEER) {
      return setupMessage
    }

    return when (session.connectionState) {
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.HOSTING,
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTING,
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.DISCONNECTED,
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.SYNCING -> {
        setupMessage ?: session.lastError
      }
      io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.CONNECTED ->
        peerTurnStatus(session)
    }
  }

  private fun handlePeerFailure(
    message: String,
    error: Throwable,
    reopenDialog: Boolean = false,
    appendErrorDetails: Boolean = true,
  ) {
    crashlytics.log(message)
    safeRecordException(error)
    setupMessage = when {
      appendErrorDetails && !error.message.isNullOrBlank() -> "$message: ${error.message}"
      else -> message
    }
    session = session?.copy(
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.DISCONNECTED,
      lastError = error.message ?: message,
    )
    if (reopenDialog) {
      newGameDialogShowing = true
    }
    suppressPeerCloseCallback = true
    viewModelScope.launch(ioDispatcher) {
      closePeerConnection()
    }
  }

  private fun onPeerConnectionClosed(error: Throwable?) {
    if (suppressPeerCloseCallback) return
    viewModelScope.launch(Dispatchers.Main) {
      releasePeerConnectionReferences()
      handlePeerDisconnect(error)
    }
  }

  private fun releasePeerConnectionReferences() {
    transportMessagesJob?.cancel()
    transportMessagesJob = null
    transport = null
    hostHandle = null
  }

  private fun handlePeerDisconnect(error: Throwable?) {
    val currentSession = session ?: return
    if (currentSession.mode != FaceToFaceSessionMode.PEER_TO_PEER) return

    if (error != null) {
      crashlytics.log("Peer connection closed")
      safeRecordException(error)
    }

    val message = when (currentSession.localRole) {
      FaceToFacePeerRole.HOST -> "Guest disconnected. Start hosting again to resume this game."
      FaceToFacePeerRole.GUEST -> "Disconnected from host. Reconnect when the host is ready to resume."
    }

    candidateMove = null
    estimateStatus = Idle
    historyIndex = null
    setupMessage = if (error?.message.isNullOrBlank()) {
      message
    } else {
      "$message ${error.message}"
    }
    session = currentSession.copy(
      connectionState = io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.DISCONNECTED,
      lastError = error?.message ?: message,
    )
  }

  private fun localDeviceName(): String {
    return runCatching { android.os.Build.MODEL }
      .getOrNull()
      ?.takeIf { it.isNotBlank() }
      ?: "Android device"
  }

  private fun safeRecordException(error: Throwable) {
    runCatching { recordException(error) }
  }

  private fun GameParameters.toSessionConfig(): FaceToFaceGameConfig {
    return FaceToFaceGameConfig(
      boardSize = size.width,
      handicap = handicap,
    )
  }

  private fun FaceToFaceSessionState?.canReconnectPeerSession(
    params: GameParameters,
    localRole: FaceToFacePeerRole,
  ): Boolean {
    return this?.mode == FaceToFaceSessionMode.PEER_TO_PEER &&
      this.localRole == localRole &&
      this.transport == FaceToFaceTransportType.WIFI_LAN &&
      this.connectionState == io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceConnectionState.DISCONNECTED &&
      this.config == params.toSessionConfig()
  }

  private fun FaceToFaceSessionState.canAcceptHandshakeSession(
    incomingSessionId: String,
  ): Boolean {
    return sessionId == incomingSessionId ||
      sessionId == PENDING_SESSION_ID ||
      incomingSessionId == PENDING_SESSION_ID
  }

  private fun FaceToFaceSessionState.matchesEstablishedSession(
    incomingSessionId: String,
  ): Boolean {
    return sessionId == incomingSessionId
  }

  private fun handleJoinConnectionFailure(
    target: FaceToFaceLanJoinTarget,
    error: Throwable,
  ) {
    val message = buildFaceToFaceLanJoinErrorMessage(
      target = target,
      error = error,
      emulatorMode = isProbablyAndroidEmulator(),
    )
    handlePeerFailure(message, error, reopenDialog = true, appendErrorDetails = false)
  }

  private fun handleHostStartFailure(error: Throwable) {
    val message = buildFaceToFaceLanHostErrorMessage(
      port = FaceToFaceLanConnectionManager.DEFAULT_PORT,
      error = error,
    )
    handlePeerFailure(message, error, reopenDialog = true, appendErrorDetails = false)
  }

  private fun hostSetupMessage(
    host: FaceToFaceLanJoinTarget,
    reconnecting: Boolean,
  ): String {
    val baseMessage = if (reconnecting) {
      "Re-hosting current game on ${host.host}:${host.port}. Reconnect from the other device."
    } else {
      "Hosting on ${host.host}:${host.port}. Join from the other device."
    }
    return if (isProbablyAndroidEmulator()) {
      "$baseMessage If the guest is another emulator, join via 10.0.2.2:${host.port}."
    } else {
      baseMessage
    }
  }

  companion object {
    private const val HOTSEAT_SESSION_ID = "face-to-face-hotseat"
    private const val PENDING_SESSION_ID = "pending-face-to-face-session"
  }

  private fun updateJoinDiscovery() {
    if (newGameDialogShowing && newGameParameters.mode == MatchMode.WIFI_JOIN) {
      startJoinDiscovery()
    } else {
      stopJoinDiscovery(clearHosts = true)
    }
  }

  private fun startJoinDiscovery() {
    if (discoveryJob?.isActive == true) return
    discoveryJob = viewModelScope.launch {
      lanDiscoveryManager.discoverHosts().collect { hosts ->
        discoveredHosts = hosts
      }
    }
  }

  private fun stopJoinDiscovery(clearHosts: Boolean) {
    discoveryJob?.cancel()
    discoveryJob = null
    if (clearHosts) {
      discoveredHosts = emptyList()
    }
  }
}

@Immutable
data class FaceToFaceState(
  val position: Position?,
  val loading: Boolean,
  val title: String,
  val buttons: List<Button>,
  val bottomText: String?,
  val gameFinished: Boolean,
  val history: List<Cell>,
  val candidateMove: Cell?,
  val boardInteractive: Boolean,
  val drawTerritory: Boolean,
  val fadeOutRemovedStones: Boolean,
  val showLastMove: Boolean,
  val koMoveDialogShowing: Boolean,
  val newGameDialogShowing: Boolean,
  val currentGameParameters: GameParameters,
  val newGameParameters: GameParameters,
  val blackPlayerLabel: String,
  val whitePlayerLabel: String,
  val setupMessage: String?,
  val discoveredHosts: List<FaceToFaceLanDiscoveredHost>,
  val extraStatus: String?,
) {
  companion object {
    val INITIAL = FaceToFaceState(
      loading = true,
      title = "Face to face · Loading",
      position = Position(19, 19),
      gameFinished = false,
      history = emptyList(),
      boardInteractive = false,
      candidateMove = null,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
      koMoveDialogShowing = false,
      buttons = emptyList(),
      bottomText = null,
      newGameDialogShowing = false,
      currentGameParameters = GameParameters(BoardSize.LARGE, 0),
      newGameParameters = GameParameters(BoardSize.LARGE, 0),
      blackPlayerLabel = "Player 1",
      whitePlayerLabel = "Player 2",
      setupMessage = null,
      discoveredHosts = emptyList(),
      extraStatus = null,
    )
  }
}

enum class MatchMode(private val title: String) {
  HOTSEAT("Same device"),
  WIFI_HOST("Wi-Fi host"),
  WIFI_JOIN("Wi-Fi join");

  override fun toString(): String = title
}

@Immutable
data class GameParameters(
  val size: BoardSize,
  val handicap: Int,
  val mode: MatchMode = MatchMode.HOTSEAT,
  val hostAddress: String = "",
)

enum class BoardSize(
  val width: Int,
  val height: Int,
  val prettyName: String,
) {
  SMALL(9, 9, "9 × 9"),
  MEDIUM(13, 13, "13 × 13"),
  LARGE(19, 19, "19 × 19");

  override fun toString(): String {
    return prettyName
  }

  companion object {
    fun fromWidth(width: Int): BoardSize {
      return entries.firstOrNull { it.width == width } ?: LARGE
    }
  }
}

sealed class Button(
  override val icon: ImageVector,
  override val label: String,
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false,
) : BottomBarButton {
  object GameSettings : Button(Icons.Rounded.AddCircle, "New Game")
  object Estimate : Button(Icons.Rounded.Functions, "Estimate Score")
  class Previous(enabled: Boolean = true) : Button(
    repeatable = true,
    enabled = enabled,
    icon = Icons.Rounded.SkipPrevious,
    label = "Previous"
  )

  class Next(enabled: Boolean = true) :
    Button(repeatable = true, enabled = enabled, icon = Icons.Rounded.SkipNext, label = "Next")

  object CloseEstimate : Button(Icons.Rounded.HighlightOff, "Return")
  class Pass(enabled: Boolean = true) :
    Button(enabled = enabled, icon = Icons.Rounded.Stop, label = "Pass")
}

sealed interface Action {
  class BoardCellDragged(val cell: Cell) : Action
  class BoardCellTapUp(val cell: Cell) : Action
  class BottomButtonPressed(val button: Button) : Action
  object KOMoveDialogDismiss : Action
  object NewGameDialogDismiss : Action
  class NewGameParametersChanged(val params: GameParameters) : Action
  object StartNewGame : Action
}

sealed interface EstimateStatus {
  object Idle : EstimateStatus
  object Working : EstimateStatus
  data class Success(val result: Position, val gameIsOver: Boolean) : EstimateStatus
}
