package io.zenandroid.onlinego.ui.screens.game

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.window.Dialog
import io.zenandroid.onlinego.data.model.ogs.ChatChannel
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.BottomBar
import io.zenandroid.onlinego.ui.composables.MoreMenuItem
import io.zenandroid.onlinego.ui.composables.TitleBar
import io.zenandroid.onlinego.ui.screens.game.Button.Analyze
import io.zenandroid.onlinego.ui.screens.game.Button.Chat
import io.zenandroid.onlinego.ui.screens.game.Button.ConfirmMove
import io.zenandroid.onlinego.ui.screens.game.Button.DiscardMove
import io.zenandroid.onlinego.ui.screens.game.Button.Estimate
import io.zenandroid.onlinego.ui.screens.game.Button.ExitAnalysis
import io.zenandroid.onlinego.ui.screens.game.Button.Next
import io.zenandroid.onlinego.ui.screens.game.Button.NextGame
import io.zenandroid.onlinego.ui.screens.game.Button.Pass
import io.zenandroid.onlinego.ui.screens.game.Button.Previous
import io.zenandroid.onlinego.ui.screens.game.Button.Resign
import io.zenandroid.onlinego.ui.screens.game.UserAction.BlackPlayerClicked
import io.zenandroid.onlinego.ui.screens.game.UserAction.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.game.UserAction.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.game.UserAction.BottomButtonPressed
import io.zenandroid.onlinego.ui.screens.game.UserAction.CancelDialogConfirm
import io.zenandroid.onlinego.ui.screens.game.UserAction.CancelDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.ChatDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.ChatSend
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameInfoClick
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameInfoDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameOverDialogAnalyze
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameOverDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameOverDialogNextGame
import io.zenandroid.onlinego.ui.screens.game.UserAction.GameOverDialogQuickReplay
import io.zenandroid.onlinego.ui.screens.game.UserAction.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.OpenInBrowser
import io.zenandroid.onlinego.ui.screens.game.UserAction.OpenVariation
import io.zenandroid.onlinego.ui.screens.game.UserAction.OpponentUndoRequestAccepted
import io.zenandroid.onlinego.ui.screens.game.UserAction.OpponentUndoRequestRejected
import io.zenandroid.onlinego.ui.screens.game.UserAction.PassDialogConfirm
import io.zenandroid.onlinego.ui.screens.game.UserAction.PassDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.PlayerDetailsDialogDismissed
import io.zenandroid.onlinego.ui.screens.game.UserAction.ResignDialogConfirm
import io.zenandroid.onlinego.ui.screens.game.UserAction.ResignDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.RetryDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.RetryDialogRetry
import io.zenandroid.onlinego.ui.screens.game.UserAction.UserUndoDialogConfirm
import io.zenandroid.onlinego.ui.screens.game.UserAction.UserUndoDialogDismiss
import io.zenandroid.onlinego.ui.screens.game.UserAction.VariationSend
import io.zenandroid.onlinego.ui.screens.game.UserAction.WhitePlayerClicked
import io.zenandroid.onlinego.ui.screens.game.composables.ChatDialog
import io.zenandroid.onlinego.ui.screens.game.composables.PlayerCard
import io.zenandroid.onlinego.ui.screens.game.composables.PlayerDetailsDialog
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun GameScreen(state: GameState,
               analysisMode: Boolean,
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
                state = state,
                onUserAction = onUserAction,
                modifier = Modifier
                    .heightIn(0.dp, (LocalConfiguration.current.screenHeightDp * .6).dp)
                    .align(CenterHorizontally)
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
                onButtonPressed = { onUserAction(BottomButtonPressed(it as Button)) },
            )
        } else {
            Row {
                Column(
                    Modifier
                        .width(0.dp)
                        .weight(1f)) {
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
                        onButtonPressed = { onUserAction(BottomButtonPressed(it as Button)) },
                    )
                }
                Board(
                    state = state,
                    onUserAction = onUserAction,
                    modifier = Modifier
                        .widthIn(0.dp, (LocalConfiguration.current.screenWidthDp * .6).dp)
                        .align(CenterVertically)
                )
            }
        }
    }
    if(state.chatDialogShowing) {
        ChatDialog(
            messages = state.messages,
            game = state.position!!,
            inAnalysisMode = analysisMode,
            onVariation = { onUserAction(OpenVariation(it)) },
            onDialogDismiss = { onUserAction(ChatDialogDismiss) },
            onSendMessage = { m, c -> onUserAction(ChatSend(m, c)) },
            onSendVariation = { onUserAction(VariationSend(it)) },
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
        PlayerDetailsDialog( { onUserAction(PlayerDetailsDialogDismissed) }, it, state.playerStats, state.versusStats, state.versusStatsHidden)
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
            .shadow(1.dp, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
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
            Text(
                text = if (state.ranked) "Ranked" else "Unranked",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onSurface,
            )
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
            Text(
                text = "Time",
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )
            Row {
                Text(
                    text = state.timerDescription ?: "",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .width(0.dp)
                        .weight(1f),
                )
            }
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
private fun Header(
    title: String,
    opponentRequestedUndo: Boolean,
    onBack: () -> Unit,
    onUserAction: (UserAction) -> Unit
) {
    val items = remember(opponentRequestedUndo) {
        val items = mutableListOf(
            MoreMenuItem("Open in browser", Rounded.OpenInBrowser) { onUserAction(OpenInBrowser) },
            MoreMenuItem("Download as SGF", Icons.Rounded.Download) { onUserAction(OpenInBrowser) },
        )
        if(opponentRequestedUndo) {
            items.add(MoreMenuItem("Accept Undo", Icons.Rounded.Undo) { onUserAction(OpponentUndoRequestAccepted) } )
        }
        items
    }
    TitleBar(
        title = title,
        titleIcon = Outlined.Info,
        onTitleClicked = { onUserAction(GameInfoClick) },
        onBack = onBack,
        moreMenuItems = items,
    )
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
        ), false, {}, {},
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
        ), false, {}, {},
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
            false,
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
            false,
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
            false,
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
            false,
            {}, {},
        )
    }
}
