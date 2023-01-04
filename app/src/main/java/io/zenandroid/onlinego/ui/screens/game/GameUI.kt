package io.zenandroid.onlinego.ui.screens.game

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.DotsFlashing
import io.zenandroid.onlinego.ui.screens.game.Button.*
import io.zenandroid.onlinego.ui.screens.game.UserAction.*
import io.zenandroid.onlinego.ui.screens.game.composables.ChatDialog
import io.zenandroid.onlinego.ui.screens.game.composables.PlayerCard
import io.zenandroid.onlinego.ui.screens.game.composables.PlayerDetailsDialog
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.*

@Composable
fun GameScreen(state: GameState,
               onUserAction: ((UserAction) -> Unit),
               onBack: (() -> Unit),
) {
    Column(Modifier.background(MaterialTheme.colors.surface)) {
        if(LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT) {
            Header(
                title = state.title,
                opponentRequestedUndo = state.opponentRequestedUndo,
                onBack = onBack,
                onUserAction = onUserAction
            )
            if (state.showAnalysisPanel) {
                Spacer(modifier = Modifier.weight(1f)) // Placeholder
            }
            if (state.showPlayers) {
                BlackPlayerCard(
                    state, onUserAction,
                    modifier = Modifier
                        .weight(.5f)
                        .fillMaxWidth()
                )
            }
            ExtraStatusField(
                text = state.blackExtraStatus,
                modifier = Modifier
                    .background(Color(0xFF867484))
                    .fillMaxWidth()
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Board(
                state, onUserAction,
                modifier = Modifier
                    .shadow(1.dp, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
            )
            ExtraStatusField(
                text = state.whiteExtraStatus,
                modifier = Modifier
                    .background(Color(0xFF867484))
                    .fillMaxWidth()
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally),
            )
            if (state.showPlayers) {
                WhitePlayerCard(
                    state, onUserAction,
                    modifier = Modifier
                        .weight(.5f)
                        .fillMaxWidth()
                )
            }
            BottomBar(
                buttons = state.buttons,
                bottomText = state.bottomText,
                onUserAction = onUserAction
            )
        } else {
            Row {
                Column(Modifier.width(0.dp).weight(1f)) {
                    Header(
                        title = state.title,
                        opponentRequestedUndo = state.opponentRequestedUndo,
                        onBack = onBack,
                        onUserAction = onUserAction
                    )
                    ExtraStatusField(
                        text = state.blackExtraStatus,
                        modifier = Modifier
                            .background(Color(0xFF867484))
                            .fillMaxWidth()
                            .padding(4.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    if (state.showPlayers) {
                        BlackPlayerCard(
                            state, onUserAction,
                            modifier = Modifier
                                .weight(.5f)
                                .fillMaxWidth()
                        )
                        WhitePlayerCard(
                            state, onUserAction,
                            modifier = Modifier
                                .weight(.5f)
                                .fillMaxWidth()
                        )
                    }
                    ExtraStatusField(
                        text = state.whiteExtraStatus,
                        modifier = Modifier
                            .background(Color(0xFF867484))
                            .fillMaxWidth()
                            .padding(4.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    BottomBar(
                        buttons = state.buttons,
                        bottomText = state.bottomText,
                        onUserAction = onUserAction
                    )
                }
                Board(
                    state, onUserAction,
                    modifier = Modifier
                        .shadow(1.dp, MaterialTheme.shapes.medium)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        }
    }
    if(state.chatDialogShowing) {
        ChatDialog(
            messages = state.messages,
            onDialogDismiss = { onUserAction(ChatDialogDismiss) },
            onSendMessage = { onUserAction(ChatSend(it)) }
        )
    }
    if (state.retryMoveDialogShowing) {
        RetryMoveDialog(onUserAction)
    }
    AnimatedVisibility (state.gameInfoDialogShowing, enter = fadeIn(), exit = fadeOut()) {
        GameInfoDialog(state, onUserAction)
    }
    if(state.koMoveDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(KOMoveDialogDismiss) },
            confirmButton = {
                TextButton(onClick = { onUserAction(KOMoveDialogDismiss) }) {
                    Text("OK")
                }
            },
            text = { Text("That move would repeat the board position. That's called a KO, and it is not allowed. Try to make another move first, preferably a threat that the opponent can't ignore.") },
            title = { Text("Illegal KO move") },
        )
    }
    if(state.opponentRequestedUndoDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(OpponentUndoRequestRejected) },
            dismissButton = {
                TextButton(onClick = { onUserAction(OpponentUndoRequestRejected) }) {
                    Text("IGNORE")
                }
            },
            confirmButton = {
                TextButton(onClick = { onUserAction(OpponentUndoRequestAccepted) }) {
                    Text("UNDO MOVE")
                }
            },
            text = { Text("Your opponent would like to undo their last move. Do you accept?") },
            title = { Text("Opponent asked to undo") },
        )
    }
    if(state.requestUndoDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(UserUndoDialogDismiss) },
            dismissButton = {
                TextButton(onClick = { onUserAction(UserUndoDialogDismiss) }) {
                    Text("CANCEL")
                }
            },
            confirmButton = {
                TextButton(onClick = { onUserAction(UserUndoDialogConfirm) }) {
                    Text("REQUEST UNDO")
                }
            },
            text = { Text("If you made the last move by mistake, you can ask your opponent if they allow you to undo it.") },
            title = { Text("Request Undo?") },
        )
    }
    if(state.passDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(PassDialogDismiss) },
            dismissButton = {
                TextButton(onClick = { onUserAction(PassDialogDismiss) }) {
                    Text("CANCEL")
                }
            },
            confirmButton = {
                TextButton(onClick = { onUserAction(PassDialogConfirm) }) {
                    Text("PASS")
                }
            },
            text = { Text("Are you sure you want to pass? You should only do this if you think the game is over and there are no more moves to be made. If your opponent passes too, the game proceeds to the scoring phase.") },
            title = { Text("Please confirm") },
        )
    }
    if(state.resignDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(ResignDialogDismiss) },
            dismissButton = {
                TextButton(onClick = { onUserAction(ResignDialogDismiss) }) {
                    Text("CANCEL")
                }
            },
            confirmButton = {
                TextButton(onClick = { onUserAction(ResignDialogConfirm) }) {
                    Text("RESIGN")
                }
            },
            text = { Text("Are you sure you want to resign?") },
            title = { Text("Please confirm") },
        )
    }
    if(state.cancelDialogShowing) {
        AlertDialog(
            onDismissRequest = { onUserAction(CancelDialogDismiss) },
            dismissButton = {
                TextButton(onClick = { onUserAction(CancelDialogDismiss) }) {
                    Text("DON'T CANCEL GAME")
                }
            },
            confirmButton = {
                TextButton(onClick = { onUserAction(CancelDialogConfirm) }) {
                    Text("CANCEL GAME")
                }
            },
            text = { Text("Are you sure you want to cancel the game?") },
            title = { Text("Please confirm") },
        )
    }
    state.gameOverDialogShowing?.let { dialog ->
        GameOverDialog(onUserAction, dialog)
    }
    state.playerDetailsDialogShowing?.let {
        PlayerDetailsDialog( { onUserAction(PlayerDetailsDialogDismissed) }, it, state.playerStats)
    }
}

@Composable
private fun BlackPlayerCard(state: GameState, onUserAction: ((UserAction) -> Unit), modifier: Modifier = Modifier) {
    PlayerCard(
        player = state.blackPlayer,
        timerMain = state.timerDetails?.blackFirstLine ?: "",
        timerExtra = state.timerDetails?.blackSecondLine ?: "",
        timerPercent = state.timerDetails?.blackPercentage ?: 0,
        timerFaded = state.timerDetails?.blackFaded ?: true,
        timerShown = state.showTimers,
        onUserClicked = { onUserAction(BlackPlayerClicked) },
        onGameDetailsClicked = { onUserAction(GameInfoClick) },
        modifier = modifier
    )
}

@Composable
private fun WhitePlayerCard(state: GameState, onUserAction: ((UserAction) -> Unit), modifier: Modifier = Modifier) {
    PlayerCard(
        player = state.whitePlayer,
        timerMain = state.timerDetails?.whiteFirstLine ?: "",
        timerExtra = state.timerDetails?.whiteSecondLine ?: "",
        timerPercent = state.timerDetails?.whitePercentage ?: 0,
        timerFaded = state.timerDetails?.whiteFaded ?: true,
        timerShown = state.showTimers,
        onUserClicked = { onUserAction(WhitePlayerClicked) },
        onGameDetailsClicked = { onUserAction(GameInfoClick) },
        modifier = modifier
    )
}

@Composable
private fun Board(state: GameState, onUserAction: ((UserAction) -> Unit), modifier: Modifier = Modifier) {
    Board(
        boardWidth = state.gameWidth,
        boardHeight = state.gameHeight,
        position = state.position,
        interactive = state.boardInteractive,
        boardTheme = state.boardTheme,
        drawCoordinates = state.showCoordinates,
        drawTerritory = state.drawTerritory,
        drawLastMove = state.showLastMove,
        lastMoveMarker = state.lastMoveMarker,
        fadeOutRemovedStones = state.fadeOutRemovedStones,
        candidateMove = state.candidateMove,
        candidateMoveType = state.position?.nextToMove,
        onTapMove = { onUserAction(BoardCellDragged(it)) },
        onTapUp = { onUserAction(BoardCellTapUp(it)) },
        modifier = modifier
    )
}

@Composable
fun ColumnScope.ExtraStatusField(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = text != null) {
        Text(
            text = text ?: "",
            style = MaterialTheme.typography.h3,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}

@Composable
private fun GameInfoDialog(state: GameState, onUserAction: (UserAction) -> Unit) {
    BackHandler { onUserAction(GameInfoDismiss) }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0x88000000))
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onUserAction(GameInfoDismiss) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(vertical = 80.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .fillMaxWidth(.9f)
                .fillMaxHeight()
                .align(Alignment.Center)
                .shadow(4.dp)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(text = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = Bold))
                append(state.blackPlayer?.name ?: "?")
                pop()
                append("      vs      ")
                pushStyle(SpanStyle(fontWeight = Bold))
                append(state.whitePlayer?.name ?: "?")
                pop()
            },
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )
            ScoreRow(state.blackScore.komi?.toString(), state.whiteScore.komi?.toString(), "komi")
            ScoreRow(state.blackScore.handicap?.toString(), state.whiteScore.handicap?.toString(), "handicap")
            ScoreRow(state.blackScore.prisoners?.toString(), state.whiteScore.prisoners?.toString(), "prisoners")
            ScoreRow(state.blackScore.stones?.toString(), state.whiteScore.stones?.toString(), "stones")
            ScoreRow(state.blackScore.territory?.toString(), state.whiteScore.territory?.toString(), "territory")
            ScoreRow(state.blackScore.total?.toInt()?.toString(), state.whiteScore.total?.toString(), "total")
        }
        Board(
            boardWidth = state.gameWidth,
            boardHeight = state.gameHeight,
            position = state.position,
            interactive = false,
            drawTerritory = false,
            drawLastMove = false,
            boardTheme = state.boardTheme,
            drawCoordinates = false,
            fadeOutRemovedStones = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 29.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.surface)
                .padding(4.dp)
                .size(124.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        )
    }
}

@Composable
private fun ScoreRow(whiteScore: String?, blackScore: String?, title: String) {
    if (whiteScore != null || blackScore != null) {
        Row {
            Text(
                text = whiteScore ?: "",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .width(0.dp)
                    .weight(1f),
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .width(0.dp)
                    .weight(1f),
            )
            Text(
                text = blackScore ?: "",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .width(0.dp)
                    .weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameInfoPreview() {
    OnlineGoTheme {
        GameInfoDialog(GameState.DEFAULT, {} )
    }
}

@Composable
private fun GameOverDialog(
    onUserAction: (UserAction) -> Unit,
    dialog: GameOverDialogDetails
) {
    Dialog(onDismissRequest = { onUserAction(GameOverDialogDismiss) }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(4.dp)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {
            val text = when {
                dialog.gameCancelled -> "GAME WAS CANCELLED"
                dialog.playerWon -> "CONGRATULATIONS\nYOU WON"
                else -> "YOU LOST"
            }
            val icon = when {
                dialog.gameCancelled -> Icons.Rounded.Cancel
                dialog.playerWon -> Icons.Rounded.ThumbUp
                else -> Icons.Rounded.ThumbDown
            }
            Text(
                text = text,
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Image(
                painter = rememberVectorPainter(image = icon),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface),
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(128.dp)
            )
            Text(
                text = dialog.detailsText,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(28.dp))
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.primaryVariant,
                    contentColor = MaterialTheme.colors.onSurface
                ),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp,
                ),
                onClick = { onUserAction(GameOverDialogAnalyze) },
            ) {
                Text(
                    text = "VIEW BOARD",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface),
                onClick = { onUserAction(GameOverDialogNextGame) },
            ) {
                Text(
                    text = "NEXT GAME",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface),
                onClick = { onUserAction(GameOverDialogQuickReplay) },
            ) {
                Text(
                    text = "QUICK REPLAY",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RetryMoveDialog(onUserAction: (UserAction) -> Unit) {
    Dialog(onDismissRequest = { onUserAction(RetryDialogDismiss) }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(4.dp)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "CONNECTION PROBLEMS",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                text = "The server is not responding. Please check your internet connection.",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(vertical = 36.dp)
            )
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.primaryVariant,
                    contentColor = MaterialTheme.colors.onSurface
                ),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp,
                ),
                onClick = { onUserAction(RetryDialogRetry) },
            ) {
                Text(
                    text = "TRY AGAIN",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface),
                onClick = { onUserAction(RetryDialogDismiss) },
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

@Composable
private fun BottomBar(
    buttons: List<Button>,
    bottomText: String?,
    onUserAction: (UserAction) -> Unit
) {
    Row(modifier = Modifier.height(56.dp)) {
        buttons.forEach {
            key(it.javaClass) {
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (it.enabled) 1f else .4f)
                            .background(
                                if (it == ConfirmMove || it == AcceptStoneRemoval) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
                            )
                            .clickable(enabled = it.enabled) {
                                if (!it.repeatable) onUserAction(BottomButtonPressed(it))
                            }
                            .repeatingClickable(
                                remember { MutableInteractionSource() },
                                it.repeatable && it.enabled
                            ) { onUserAction(BottomButtonPressed(it)) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            it.getIcon(),
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colors.onSurface,
                        )
                        Text(
                            text = it.getLabel(),
                            style = MaterialTheme.typography.h5,
                            color = MaterialTheme.colors.onSurface,
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = it.bubbleText != null,
                        enter = fadeIn(TweenSpec( 500)) + scaleIn(SpringSpec(Spring.DampingRatioHighBouncy)),
                        exit = fadeOut() + shrinkOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                    ) {
                        it.bubbleText?.let { bubble ->
                            Text(
                                text = bubble,
                                fontSize = 9.sp,
                                fontWeight = Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.surface,
                                modifier = Modifier
                                    .padding(start = 19.dp, top = 5.dp)
                                    .background(MaterialTheme.colors.primary, CircleShape)
                                    .size(16.dp)
                                    .wrapContentHeight(),

                                )
                        }
                    }
                }
            }
        }
        bottomText?.let { text ->
            Spacer(modifier = Modifier.weight(.5f))
            Text(
                text = text,
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            DotsFlashing(
                dotSize = 4.dp,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(top = 10.dp, start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(.5f))
        }
    }
}

@Composable
private fun Header(
    title: String,
    opponentRequestedUndo: Boolean,
    onBack: () -> Unit,
    onUserAction: (UserAction) -> Unit
) {
    Row {
        IconButton(onClick = { onBack() }) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colors.onSurface)
        }
        Spacer(modifier = Modifier.weight(.5f))
        Text(
            text = title,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.h3,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable { onUserAction(GameInfoClick) }
        )
        Icon(
            Icons.Outlined.Info,
            "Game Info",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterVertically)
                .padding(start = 6.dp)
                .clickable { onUserAction(GameInfoClick) }
        )
        Spacer(modifier = Modifier.weight(.5f))
        Box {
            var moreMenuOpen by rememberSaveable { mutableStateOf(false) }
            IconButton(onClick = { moreMenuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, "More", tint = MaterialTheme.colors.onSurface)
            }
            DropdownMenu(
                expanded = moreMenuOpen,
                onDismissRequest = { moreMenuOpen = false },
            ) {
                DropdownMenuItem(onClick = {
                    moreMenuOpen = false
                    onUserAction(OpenInBrowser)
                }) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Rounded.OpenInBrowser),
                        contentDescription = "Open in browser",
                        tint = MaterialTheme.colors.onSurface,
                    )
                    Text(
                        text = "Open in browser",
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                DropdownMenuItem(onClick = {
                    moreMenuOpen = false
                    onUserAction(DownloadSGF)
                }) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Rounded.Download),
                        contentDescription = "Download as SGF",
                        tint = MaterialTheme.colors.onSurface,
                    )
                    Text(
                        text = "Download as SGF",
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if(opponentRequestedUndo) {
                    DropdownMenuItem(onClick = {
                        moreMenuOpen = false
                        onUserAction(OpponentUndoRequestAccepted)
                    }) {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Rounded.Undo),
                            contentDescription = "Accept Undo",
                            tint = MaterialTheme.colors.onSurface,
                        )
                        Text(
                            text = "Accept Undo",
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Button.getIcon() = when(this) {
    ConfirmMove -> Icons.Rounded.ThumbUp
    DiscardMove -> Icons.Rounded.Cancel
    Analyze -> Icons.Rounded.Biotech
    Pass -> Icons.Rounded.Stop
    Resign -> Icons.Rounded.OutlinedFlag
    CancelGame -> Icons.Rounded.Cancel
    is Chat -> Icons.Rounded.Forum
    is NextGame -> Icons.Rounded.NextPlan
    Undo -> Icons.Rounded.Undo
    ExitAnalysis -> Icons.Rounded.HighlightOff
    ExitEstimate -> Icons.Rounded.HighlightOff
    is Estimate -> Icons.Rounded.Functions
    Previous -> Icons.Rounded.SkipPrevious
    is Next -> Icons.Rounded.SkipNext
    AcceptStoneRemoval -> Icons.Rounded.ThumbUp
    RejectStoneRemoval -> Icons.Rounded.ThumbDown
}

private fun Button.getLabel() = when(this) {
    AcceptStoneRemoval -> "Accept"
    RejectStoneRemoval -> "Reject"
    ExitEstimate -> "Return"
    Analyze -> "Analyze"
    CancelGame -> "Cancel Game"
    is Chat -> "Chat"
    ConfirmMove -> "Confirm Move"
    DiscardMove -> "Discard Move"
    is Estimate -> "Estimate"
    ExitAnalysis -> "Exit Analysis"
    is Next -> "Next"
    is NextGame -> "Next Game"
    Pass -> "Pass"
    Previous -> "Previous"
    Resign -> "Resign"
    Undo -> "Undo"
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(Analyze, Pass, Resign, Chat(), NextGame()),
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
                timeLeft = 1000,
            ),
        ), {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview1() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(ConfirmMove, DiscardMove),
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
                timeLeft = 1000,
                ),
        ), {}, {},
        )
    }
}

@Preview(showBackground = true)
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
                timeLeft = 1000,
                ),
            bottomText = "Submitting move",
        ),
            {}, {},
        )
    }
}
@Preview(showBackground = true)
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
                timeLeft = 1000,
                ),
            bottomText = "Submitting move",
            retryMoveDialogShowing = true,
        ),
            {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview4() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(ExitAnalysis, Estimate(), Previous, Next()),
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
                timeLeft = 1000,
                ),
            showPlayers = false,
            showAnalysisPanel = true,
        ),
            {}, {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview5() {
    OnlineGoTheme {
        GameScreen(state = GameState.DEFAULT.copy(
            position = Position(19, 19, whiteStones = setOf(Cell(3, 3)), blackStones = setOf(Cell(15, 15))),
            loading = false,
            buttons = listOf(ExitAnalysis, Estimate(), Previous, Next()),
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
                timeLeft = 1000,
                ),
            showPlayers = false,
            showAnalysisPanel = true,
            gameOverDialogShowing = GameOverDialogDetails(
                gameCancelled = false,
                playerWon = false,
                detailsText = buildAnnotatedString {
                    pushStyle(SpanStyle(fontWeight = Bold))
                    append("MrAlex")
                    pop()
                    append(" resigned on move 132")
                }
            ),
        ),
            {}, {},
        )
    }
}