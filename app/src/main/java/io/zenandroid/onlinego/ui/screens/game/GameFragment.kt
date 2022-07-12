package io.zenandroid.onlinego.ui.screens.game

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import coil.compose.rememberImagePainter
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.DotsFlashing
import io.zenandroid.onlinego.ui.screens.game.Button.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

const val GAME_ID = "GAME_ID"
const val GAME_WIDTH = "GAME_WIDTH"
const val GAME_HEIGHT = "GAME_HEIGHT"

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModel()
    private val stoneSoundMediaPlayer = MediaPlayer.create(OnlineGoApplication.instance, R.raw.stone)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.initialize(
            gameId = requireArguments().getLong(GAME_ID),
            gameWidth = requireArguments().getInt(GAME_WIDTH),
            gameHeight = requireArguments().getInt(GAME_HEIGHT),
        )

        lifecycleScope.launch {
            viewModel.events.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                when(it) {
                    Event.PlayStoneSound -> stoneSoundMediaPlayer.start()
                    null -> {}
                }
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                viewModel.pendingNavigation ?.let { nav ->
                    when(nav) {
                        is PendingNavigation.NavigateToGame -> navigateToGameScreen(nav.game)
                    }
                }

                val state by rememberStateWithLifecycle(viewModel.state)

                OnlineGoTheme {
                    GameScreen(
                        state = state,
                        onTapMove = viewModel::onCellTracked,
                        onTapUp = viewModel::onCellTapUp,
                        onButtonPressed = viewModel::onButtonPressed,
                        onBack = ::onBackPressed,
                        onGameInfo = {},
                        onMore = {},
                        onRetryDialogDismissed = viewModel::onRetryDialogDismissed,
                        onRetryDialogRetry = viewModel::onRetryDialogRetry,
                        onPassDialogConfirm = viewModel::onPassDialogConfirm,
                        onPassDialogDismissed = viewModel::onPassDialogDismissed,
                        onResignDialogConfirm = viewModel::onResignDialogConfirm,
                        onResignDialogDismissed = viewModel::onResignDialogDismissed,
                        onGameOverDialogAnalyze = viewModel::onGameOverDialogAnalyze,
                        onGameOverDialogDismissed = viewModel::onGameOverDialogDismissed,
                        onGameOverDialogNextGame = viewModel::onGameOverDialogNextGame,
                        onChatDialogDismissed = viewModel::onChatDialogDismissed,
                        onSendChat = viewModel::onSendChat,
                    )
                }
            }
        }
    }

    private fun navigateToGameScreen(game: Game) {
        view?.findNavController()
            ?.navigate(
                R.id.gameFragment,
                bundleOf(GAME_ID to game.id, GAME_WIDTH to game.width, GAME_HEIGHT to game.height),
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.gameFragment, true)
                    .build()
            )
    }

    private fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}

@Composable
fun GameScreen(state: GameState,
               onTapMove: ((Cell) -> Unit),
               onTapUp: ((Cell) -> Unit),
               onButtonPressed: ((Button) -> Unit),
               onBack: (() -> Unit),
               onGameInfo: (() -> Unit),
               onMore: (() -> Unit),
               onRetryDialogDismissed: (() -> Unit),
               onRetryDialogRetry: (() -> Unit),
               onPassDialogDismissed: (() -> Unit),
               onPassDialogConfirm: (() -> Unit),
               onResignDialogDismissed: (() -> Unit),
               onResignDialogConfirm: (() -> Unit),
               onGameOverDialogDismissed: (() -> Unit),
               onGameOverDialogAnalyze: (() -> Unit),
               onGameOverDialogNextGame: (() -> Unit),
               onChatDialogDismissed: (() -> Unit),
               onSendChat: ((String) -> Unit),
) {
    Box {
        Column(Modifier.background(Color.White)) {
            Row {
                IconButton(onClick = { onBack.invoke() }) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = Color(0xFF443741))
                }
                Spacer(modifier = Modifier.weight(.5f))
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.h3,
                    color = Color(0xFF443741),
                    modifier = Modifier.align(CenterVertically)
                )
                Icon(
                    Icons.Outlined.Info, "Game Info", tint = Color(0xFF443741), modifier = Modifier
                        .size(18.dp)
                        .align(CenterVertically)
                        .padding(start = 6.dp)
                )
                Spacer(modifier = Modifier.weight(.5f))
                IconButton(onClick = { onMore.invoke() }) {
                    Icon(Icons.Rounded.MoreVert, "More", tint = Color(0xFF443741))
                }
            }
            if (state.showAnalysisPanel) {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (state.showPlayers) {
                PlayerCard(
                    player = state.blackPlayer,
                    timerMain = state.timerDetails?.blackFirstLine ?: "",
                    timerExtra = state.timerDetails?.blackSecondLine ?: "",
                    timerPercent = state.timerDetails?.blackPercentage ?: 0,
                    timerFaded = state.timerDetails?.blackFaded ?: true,
                    modifier = Modifier
                        .weight(.5f)
                        .fillMaxWidth()
                )
            }
            if(state.blackExtraStatus != null) {
                Text(
                    text = state.blackExtraStatus,
                    style = MaterialTheme.typography.h3,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xFF867484))
                        .fillMaxWidth()
                        .padding(4.dp)
                        .align(CenterHorizontally),
                )
            }
            Board(
                boardWidth = state.gameWidth,
                boardHeight = state.gameHeight,
                position = state.position,
                interactive = state.boardInteractive,
                drawTerritory = state.drawTerritory,
                fadeOutRemovedStones = state.fadeOutRemovedStones,
                candidateMove = state.candidateMove,
                candidateMoveType = state.position?.nextToMove,
                onTapMove = onTapMove,
                onTapUp = onTapUp,
            )
            if(state.whiteExtraStatus != null) {
                Text(
                    text = state.whiteExtraStatus,
                    style = MaterialTheme.typography.h3,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xFF867484))
                        .fillMaxWidth()
                        .padding(4.dp)
                        .align(CenterHorizontally),
                )
            }
            if (state.showPlayers) {
                PlayerCard(
                    player = state.whitePlayer,
                    timerMain = state.timerDetails?.whiteFirstLine ?: "",
                    timerExtra = state.timerDetails?.whiteSecondLine ?: "",
                    timerPercent = state.timerDetails?.whitePercentage ?: 0,
                    timerFaded = state.timerDetails?.whiteFaded ?: true,
                    modifier = Modifier
                        .weight(.5f)
                        .fillMaxWidth()
                )
            }
            Row(modifier = Modifier.height(56.dp)) {
                state.buttons.forEach {
                    key(it) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .alpha(if (it.enabled) 1f else .4f)
                                .weight(1f)
                                .background(
                                    if (it == CONFIRM_MOVE || it == ACCEPT_STONE_REMOVAL) Color(
                                        0xFFFEDF47
                                    ) else Color.White
                                )
                                .clickable(enabled = it.enabled) {
                                    if (!it.repeatable) onButtonPressed.invoke(it)
                                }
                                .repeatingClickable(
                                    remember { MutableInteractionSource() },
                                    it.repeatable && it.enabled
                                ) { onButtonPressed.invoke(it) },
                            horizontalAlignment = CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                it.getIcon(),
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF443741),
                            )
                            Text(
                                text = it.getLabel(),
                                style = MaterialTheme.typography.h5,
                                color = Color(0xFF443741),
                            )
                        }
                    }
                }
                state.bottomText?.let { text ->
                    Spacer(modifier = Modifier.weight(.5f))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.h2,
                        modifier = Modifier.align(CenterVertically)
                    )
                    DotsFlashing(
                        dotSize = 4.dp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(top = 10.dp, start = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(.5f))
                }
            }
        }
        if(state.chatDialogShowing) {
            BackHandler { onChatDialogDismissed() }
            Spacer(modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000))
                .clickable { onChatDialogDismissed() }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .fillMaxWidth(.9f)
                    .fillMaxHeight(.9f)
                    .align(Center)
                    .shadow(4.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(16.dp)
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect(state.messages) {
                    val index = state.messages.values.fold(state.messages.keys.size) { count, list -> count + list.size } - 1
                    listState.animateScrollToItem(index.coerceAtLeast(0))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    state.messages.keys.sortedBy { it }.forEach { moveNo ->
                        stickyHeader {
                            Text(
                                text = "Move $moveNo",
                                style = MaterialTheme.typography.h5,
                                color = Color(0xFF867484),
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                        items(state.messages[moveNo] ?: emptyList()) {
                            if(it.fromUser) {
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = it.message.text,
                                        color = Color(0xFF443741),
                                        style = MaterialTheme.typography.body2,
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFF443741),
                                                shape = RoundedCornerShape(24.dp, 0.dp, 24.dp, 24.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            } else {
                                Row(modifier = Modifier
                                    .fillParentMaxWidth()
                                    .padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = it.message.text,
                                        color = Color.White,
                                        style = MaterialTheme.typography.body2,
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFF443741),
                                                shape = RoundedCornerShape(0.dp, 24.dp, 24.dp, 24.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row {
                    var message by rememberSaveable { mutableStateOf("") }
                    TextField(
                        value = message,
                        onValueChange = { message = it},
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        onSendChat(message)
                        message = ""
                    }) {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Rounded.Send),
                            tint = Color(0xFF443741),
                            contentDescription = "send",
                        )
                    }
                }
            }
        }
    }
    if (state.retryMoveDialogShown) {
        Dialog(onDismissRequest = onRetryDialogDismissed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .shadow(4.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "CONNECTION PROBLEMS",
                    style = MaterialTheme.typography.h6,
                    color = Color(0xFF443741),
                    )
                Text(
                    text = "The server is not responding. Please check your internet connection.",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF443741),
                    modifier = Modifier.padding(vertical = 36.dp)
                )
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = Color(0xFFFEDF47),
                        contentColor = Color(0xFF443741)
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp,
                    ),
                    onClick = onRetryDialogRetry,
                ) {
                    Text(
                        text = "TRY AGAIN",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF443741)),
                    onClick = onRetryDialogDismissed,
                ) {
                    Text(
                        text = "CANCEL",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    if(state.passDialogShowing) {
        AlertDialog(
            onDismissRequest = onPassDialogDismissed,
            dismissButton = {
                TextButton(onClick = onPassDialogDismissed) {
                    Text("CANCEL")
                }
            },
            confirmButton = {
                TextButton(onClick = onPassDialogConfirm) {
                    Text("PASS")
                }
            },
            text = { Text("Are you sure you want to pass? You should only do this if you think the game is over and there are no more moves to be made. If your opponent passes too, the game proceeds to the scoring phase.") },
            title = { Text("Please confirm") },
        )
    }
    if(state.resignDialogShowing) {
        AlertDialog(
            onDismissRequest = onPassDialogDismissed,
            dismissButton = {
                TextButton(onClick = onResignDialogDismissed) {
                    Text("CANCEL")
                }
            },
            confirmButton = {
                TextButton(onClick = onResignDialogConfirm) {
                    Text("RESIGN")
                }
            },
            text = { Text("Are you sure you want to resign?") },
            title = { Text("Please confirm") },
        )
    }
    state.gameOverDialogShowing?.let { dialog ->
        Dialog(onDismissRequest = onGameOverDialogDismissed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .shadow(4.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if(dialog.playerWon) "CONGRATULATIONS\nYOU WON" else "YOU LOST",
                    style = MaterialTheme.typography.h2,
                    color = Color(0xFF443741),
                    textAlign = TextAlign.Center,
                )
                Image(
                    painter = rememberVectorPainter(image = if(dialog.playerWon) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbDown),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(Color(0xFF443741)),
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .size(128.dp)
                )
                Text(
                    text = dialog.detailsText,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF443741),
                )
                Spacer(modifier = Modifier.height(28.dp))
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = Color(0xFFFEDF47),
                        contentColor = Color(0xFF443741)
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp,
                    ),
                    onClick = onGameOverDialogAnalyze,
                ) {
                    Text(
                        text = "ANALYZE",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF443741)),
                    onClick = onGameOverDialogNextGame,
                ) {
                    Text(
                        text = "NEXT GAME",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerCard(player: PlayerData?, timerMain: String, timerExtra: String, timerPercent: Int, timerFaded: Boolean, modifier: Modifier = Modifier) {
    val alpha = if(timerFaded) .6f else 1f
    player?.let {
        Row(verticalAlignment = CenterVertically,
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(start = 18.dp)){
                Canvas(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                        .align(CenterHorizontally)
                        .alpha(alpha)
                ) {
                    drawCircle(
                        color = Color(0xFF443741),
                        radius = 14.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFF443741),
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()),
                        startAngle = -90f,
                        sweepAngle = -360f * timerPercent / 100f,
                        useCenter = true,
                    )
                }
                Text(
                    text = timerMain,
                    style = MaterialTheme.typography.h6,
                    color = Color(0xFF443741).copy(alpha = alpha),
                )
                Text(
                    text = timerExtra,
                    style = MaterialTheme.typography.h5,
                    color = Color(0xFF443741).copy(alpha = alpha),
                )
            }
            Box(modifier = Modifier
                .padding(start = 16.dp)
                .size(64.dp)
            ) {
                Image(
                    painter = rememberImagePainter(
                        data = processGravatarURL(player.iconURL, LocalDensity.current.run { 60.dp.roundToPx() }),
                        builder = {
                            placeholder(R.mipmap.placeholder)
                            error(R.mipmap.placeholder)
                        }
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp, end = 4.dp)
                        .clip(CircleShape)
                )
                Box(modifier = Modifier
                    .align(BottomEnd)
                    .padding(end = 4.dp)
                ) {
                    val shield =
                        if (player.color == StoneType.BLACK) R.drawable.black_shield else R.drawable.white_shield
                    Image(
                        painter = painterResource(id = shield),
                        contentDescription = null,
                    )
                    Text(
                        text = player.rank,
                        style = MaterialTheme.typography.h5,
                        color = if (player.color == StoneType.WHITE) Color(0xFF443741) else Color.White,
                        modifier = Modifier
                            .align(Center)
                            .padding(bottom = 5.dp, end = 1.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = player.name + "  " + player.flagCode,
                    style = MaterialTheme.typography.h2,
                    color = Color(0xFF443741),
                    modifier = Modifier
                )
                Text(
                    text = player.details,
                    style = MaterialTheme.typography.h5.copy(fontSize = 10.sp),
                    color = Color(0xFF443741),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

fun Modifier.repeatingClickable(
    interactionSource: InteractionSource,
    enabled: Boolean,
    maxDelayMillis: Long = 300,
    minDelayMillis: Long = 20,
    delayDecayFactor: Float = .15f,
    onClick: () -> Unit
): Modifier = composed {

    val currentClickListener by rememberUpdatedState(onClick)

    pointerInput(interactionSource, enabled) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val heldButtonJob = launch {
                        var currentDelayMillis = maxDelayMillis
                        while (enabled && down.pressed) {
                            currentClickListener()
                            delay(currentDelayMillis)
                            val nextMillis = currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                            currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
                        }
                    }
                    waitForUpOrCancellation()
                    heldButtonJob.cancel()
                }
            }
        }
    }
}

private fun Button.getIcon() = when(this) {
    CONFIRM_MOVE -> Icons.Rounded.ThumbUp
    DISCARD_MOVE -> Icons.Rounded.Cancel
    ANALYZE -> Icons.Rounded.Biotech
    PASS -> Icons.Rounded.Stop
    RESIGN -> Icons.Rounded.OutlinedFlag
    CHAT -> Icons.Rounded.Forum
    NEXT_GAME, NEXT_GAME_DISABLED -> Icons.Rounded.NextPlan
    UNDO -> Icons.Rounded.Undo
    EXIT_ANALYSIS -> Icons.Rounded.HighlightOff
    ESTIMATE -> Icons.Rounded.Functions
    PREVIOUS -> Icons.Rounded.SkipPrevious
    NEXT -> Icons.Rounded.SkipNext
    ACCEPT_STONE_REMOVAL -> Icons.Rounded.ThumbUp
    REJECT_STONE_REMOVAL -> Icons.Rounded.ThumbDown
}

private fun Button.getLabel() = when(this) {
    NEXT_GAME_DISABLED -> "Next game"
    ACCEPT_STONE_REMOVAL -> "Accept"
    REJECT_STONE_REMOVAL -> "Reject"
    else -> name.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        .replace('_', ' ')
}

@Preview (showBackground = true)
@Composable
fun Preview() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(ANALYZE, PASS, RESIGN, CHAT, NEXT_GAME),
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
        ), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview (showBackground = true)
@Composable
fun Preview1() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(CONFIRM_MOVE, DISCARD_MOVE),
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
        ), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview (showBackground = true)
@Composable
fun Preview2() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
            bottomText = "Submitting move",
        ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
@Preview (showBackground = true)
@Composable
fun Preview3() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
            bottomText = "Submitting move",
            retryMoveDialogShown = true,
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview (showBackground = true)
@Composable
fun Preview4() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(EXIT_ANALYSIS, ESTIMATE, PREVIOUS, NEXT),
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
            showPlayers = false,
            showAnalysisPanel = true,
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview (showBackground = true)
@Composable
fun Preview5() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(EXIT_ANALYSIS, ESTIMATE, PREVIOUS, NEXT),
            title = "Move 132 · Chinese · Black",
            whitePlayer = PlayerData(
                name = "MrAlex-test",
                details = "+5.5 points",
                rank = "13k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.WHITE,
            ),
            blackPlayer = PlayerData(
                name = "MrAlex",
                details = "",
                rank = "9k",
                flagCode = "\uD83C\uDDEC\uD83C\uDDE7",
                iconURL = "https://secure.gravatar.com/avatar/d740835c39d6dd7c60977b244ac821db?s=64&d=retro",
                color = StoneType.BLACK,
                ),
            timerDetails = TimerDetails(
                whiteFirstLine = "04:26",
                whiteSecondLine = "+ 3 × 01:00",
                whitePercentage = 80,
                whiteFaded = true,
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
                blackFaded = false,
                whiteStartTimer = null,
                blackStartTimer = null,
            ),
            showPlayers = false,
            showAnalysisPanel = true,
            gameOverDialogShowing = GameOverDialogDetails(
                playerWon = false,
                detailsText = buildAnnotatedString {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("MrAlex")
                    pop()
                    append(" resigned on move 132")
                }
            ),
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}