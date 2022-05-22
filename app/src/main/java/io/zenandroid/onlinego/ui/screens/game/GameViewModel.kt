package io.zenandroid.onlinego.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.GameConnection
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.game.Button.*
import io.zenandroid.onlinego.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val activeGamesRepository: ActiveGamesRepository,
    userSessionRepository: UserSessionRepository,
    private val clockDriftRepository: ClockDriftRepository,
    private val socketService: OGSWebSocketService,
): ViewModel() {

    lateinit var state: StateFlow<GameState>
    private val loading = MutableStateFlow(true)
    private lateinit var position: MutableStateFlow<Position>
    private val userId = userSessionRepository.userId
    private val candidateMove = MutableStateFlow<Cell?>(null)
    private lateinit var gameConnection: GameConnection
    private val gameState = MutableStateFlow<Game?>(null)
    private val timer = MutableStateFlow(TimerDetails("", "", "", "", 0, 0))

    fun initialize(gameId: Long, gameWidth: Int, gameHeight: Int) {
        val gameFlow = activeGamesRepository.monitorGameFlow(gameId).distinctUntilChanged()
        position = MutableStateFlow(Position(gameWidth, gameHeight))
        gameConnection = socketService.connectToGame(gameId, true)

        viewModelScope.launch {
            gameFlow.collect { game ->
                withContext(Dispatchers.IO) {
                    position.emit(RulesManager.replay(game))
                    gameState.emit(game)
                    loading.emit(false)
                    viewModelScope.launch {
                        timerRefresher()
                    }
                }
            }
        }


        state = combine(
            loading,
            gameState,
            position,
            timer,
            candidateMove,
        ) { loading, game, position, timer, candidateMove ->
            val isMyTurn = game?.phase == Phase.PLAY && (position.nextToMove == StoneType.WHITE && game.whitePlayer.id == userId) || (position.nextToMove == StoneType.BLACK && game?.blackPlayer?.id == userId)
            val visibleButtons =
                when {
                    isMyTurn && candidateMove == null -> listOf(ANALYZE, PASS, RESIGN, CHAT, NEXT_GAME)
                    isMyTurn && candidateMove != null -> listOf(CONFIRM_MOVE, DISCARD_MOVE)
                    !isMyTurn && game?.phase == Phase.PLAY -> listOf(ANALYZE, UNDO, RESIGN, CHAT, NEXT_GAME)
                    else -> emptyList()
                }

            val whiteToMove = game?.playerToMoveId == game?.whitePlayer?.id
            GameState(
                position = position,
                loading = loading,
                gameWidth = gameWidth,
                gameHeight = gameHeight,
                candidateMove = candidateMove,
                boardInteractive = isMyTurn,
                buttons = visibleButtons,
                title = if(loading) "Loading..." else "Move ${game?.moves?.size} · ${game?.rules?.capitalize()} · ${if(whiteToMove) "White" else "Black"}",
                whitePlayer = game?.whitePlayer?.data(StoneType.WHITE),
                blackPlayer = game?.blackPlayer?.data(StoneType.BLACK),
                timerDetails = timer,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameState(
                position = null,
                loading = true,
                gameWidth = gameWidth,
                gameHeight = gameHeight,
                candidateMove = null,
                boardInteractive = false,
                buttons = emptyList(),
                title = "Loading...",
                whitePlayer = null,
                blackPlayer = null,
                timerDetails = null,
            ),
        )
    }

    private suspend fun timerRefresher() {
        while (true) {
            var delayUntilNextUpdate = 1000L
            gameState.value?.let { game ->
                game.clock?.let { clock ->
                    val whiteToMove = game.playerToMoveId == game.whitePlayer.id
                    val blackToMove = game.playerToMoveId == game.blackPlayer.id

                    val whiteTimer = computeTimeLeft(
                        clock,
                        clock.whiteTimeSimple,
                        clock.whiteTime,
                        whiteToMove,
                        game.pausedSince
                    )
                    val blackTimer = computeTimeLeft(
                        clock,
                        clock.blackTimeSimple,
                        clock.blackTime,
                        blackToMove,
                        game.pausedSince
                    )

                    var timeLeft = null as Long?

                    if (clock.startMode == true) {
                        clock.expiration?.let { expiration ->
                            timeLeft = expiration - clockDriftRepository.serverTime
                            timer.emit(
                                if (whiteToMove)
                                    TimerDetails(
                                        whiteFirstLine = formatMillis(timeLeft!!),
                                        whiteSecondLine = "(start)",
                                        whitePercentage = 45,
                                        blackFirstLine = blackTimer.firstLine ?: "",
                                        blackSecondLine = blackTimer.secondLine ?: "",
                                        blackPercentage = 45,
                                    )
                                else
                                    TimerDetails(
                                        whiteFirstLine = whiteTimer.firstLine ?: "",
                                        whiteSecondLine = whiteTimer.secondLine ?: "",
                                        whitePercentage = 45,
                                        blackFirstLine = formatMillis(timeLeft!!),
                                        blackSecondLine = "(start)",
                                        blackPercentage = 45,
                                    )
                            )
                        }
                    } else {
                        if ((game.phase == Phase.PLAY || game.phase == Phase.STONE_REMOVAL) && !loading.value) {

                            timer.emit(
                                TimerDetails(
                                    whiteFirstLine = whiteTimer.firstLine ?: "",
                                    whiteSecondLine = whiteTimer.secondLine ?: "",
                                    whitePercentage = 45,
                                    blackFirstLine = blackTimer.firstLine ?: "",
                                    blackSecondLine = blackTimer.secondLine ?: "",
                                    blackPercentage = 45,
                                )
                            )


                            timeLeft = if (whiteToMove) whiteTimer.timeLeft else blackTimer.timeLeft

                        }
                    }
                    delayUntilNextUpdate = timeLeft?.let {
                        when (it) {
                            in 0 until 10_000 -> 100
                            in 10_000 until 3_600_000 -> 1_000
                            in 3_600_000 until 24 * 3_600_000 -> 60_000
                            else -> null
                        }
                    } ?: 1000
                }
            }

            delay(delayUntilNextUpdate)
        }
    }

    private fun Player.data(color: StoneType): PlayerData {
        return PlayerData(
            name = username,
            details = "TODO",
            rank = formatRank(egfToRank(rating)),
            flagCode = convertCountryCodeToEmojiFlag(country),
            iconURL = icon,
            color = color,
        )
    }

    override fun onCleared() {
        gameConnection.close()
        super.onCleared()
    }

    fun onCellTracked(cell: Cell) {
        if(!position.value.blackStones.contains(cell) && !position.value.whiteStones.contains(cell)) {
            candidateMove.value = cell
        }
    }

    fun onCellTapUp(cell: Cell) {
        viewModelScope.launch {
            val newPosition = RulesManager.makeMove(position.value, position.value.nextToMove, cell)
            if(newPosition == null) {
                candidateMove.emit(null)
            }
        }
    }

    fun onButtonPressed(button: Button) {
        when(button) {
            CONFIRM_MOVE -> candidateMove.value?.let { gameConnection.submitMove(it) }
            DISCARD_MOVE -> candidateMove.value = null
            ANALYZE -> TODO()
            PASS -> TODO()
            RESIGN -> TODO()
            CHAT -> TODO()
            NEXT_GAME -> TODO()
            UNDO -> TODO()
        }
    }
}

data class GameState(
    val position: Position?,
    val loading: Boolean,
    val gameWidth: Int,
    val gameHeight: Int,
    val candidateMove: Cell?,
    val boardInteractive: Boolean,
    val buttons: List<Button>,
    val title: String,
    val whitePlayer: PlayerData?,
    val blackPlayer: PlayerData?,
    val timerDetails: TimerDetails?,
)

data class PlayerData(
    val name: String,
    val details: String,
    val rank: String,
    val flagCode: String,
    val iconURL: String?,
    val color: StoneType,
)

enum class Button {
    CONFIRM_MOVE,
    DISCARD_MOVE,
    ANALYZE,
    PASS,
    RESIGN,
    CHAT,
    NEXT_GAME,
    UNDO,
}

data class TimerDetails(
    val whiteFirstLine: String,
    val blackFirstLine: String,
    val whiteSecondLine: String,
    val blackSecondLine: String,
    val whitePercentage: Int,
    val blackPercentage: Int,
)