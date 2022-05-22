package io.zenandroid.onlinego.ui.screens.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.rememberImagePainter
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.screens.game.Button.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.ui.theme.background
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

const val GAME_ID = "GAME_ID"
const val GAME_WIDTH = "GAME_WIDTH"
const val GAME_HEIGHT = "GAME_HEIGHT"

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModel()

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
        return ComposeView(requireContext()).apply {
            setContent {
                val state by rememberStateWithLifecycle(viewModel.state)

                OnlineGoTheme {
                    GameScreen(state, viewModel::onCellTracked, viewModel::onCellTapUp, viewModel::onButtonPressed, ::onBackPressed)
                }
            }
        }
    }

    private fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}

@Composable
fun GameScreen(state: GameState,
               onTapMove: ((Cell) -> Unit)? = null,
               onTapUp: ((Cell) -> Unit)? = null,
               onButtonPressed: ((Button) -> Unit)? = null,
               onBack: (() -> Unit)? = null,
               onGameInfo: (() -> Unit)? = null,
               onMore: (() -> Unit)? = null,
) {
    Column (Modifier.background(Color.White)){
        Row {
            IconButton(onClick = { onBack?.invoke() }) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color(0xFF443741))
            }
            Spacer(modifier = Modifier.weight(.5f))
            Text(
                text = state.title,
                style = MaterialTheme.typography.h3,
                color = Color(0xFF443741),
                modifier = Modifier.align(CenterVertically)
            )
            Icon(Icons.Outlined.Info, "Game Info", tint = Color(0xFF443741), modifier = Modifier
                .size(18.dp)
                .align(CenterVertically)
                .padding(start = 6.dp))
            Spacer(modifier = Modifier.weight(.5f))
            IconButton(onClick = { onMore?.invoke() }) {
                Icon(Icons.Rounded.MoreVert, "More", tint = Color(0xFF443741))
            }
        }
        PlayerCard(
            player = state.blackPlayer,
            timerMain = state.timerDetails?.blackFirstLine ?: "",
            timerExtra = state.timerDetails?.blackSecondLine ?: "",
            timerPercent = state.timerDetails?.blackPercentage ?: 0,
            modifier = Modifier
                .weight(.5f)
                .fillMaxWidth()
        )
        Board(
            boardWidth = state.gameWidth,
            boardHeight = state.gameHeight,
            position = state.position,
            interactive = state.boardInteractive,
            candidateMove = state.candidateMove,
            candidateMoveType = state.position?.nextToMove,
            onTapMove = onTapMove,
            onTapUp = onTapUp,
            modifier = Modifier.let { if(LocalInspectionMode.current) it.background(Color(0xFFFFcc55)) else it }
        )
        PlayerCard(
            player = state.whitePlayer,
            timerMain = state.timerDetails?.whiteFirstLine ?: "",
            timerExtra = state.timerDetails?.whiteSecondLine ?: "",
            timerPercent = state.timerDetails?.whitePercentage ?: 0,
            modifier = Modifier
                .weight(.5f)
                .fillMaxWidth()
        )
        Row (modifier = Modifier.height(56.dp)) {
            state.buttons.forEach {
                key(it) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(if(it == CONFIRM_MOVE) Color(0xFFFEDF47) else Color.White)
                            .clickable { onButtonPressed?.invoke(it) },
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
                            text = it.name.lowercase().capitalize(Locale.ROOT).replace('_', ' '),
                            style = MaterialTheme.typography.h5,
                            color = Color(0xFF443741),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCard(player: PlayerData?, timerMain: String, timerExtra: String, timerPercent: Int, modifier: Modifier = Modifier) {
    player?.let {
        Row(verticalAlignment = CenterVertically,
            modifier = modifier
        ) {
            Column(modifier = Modifier.padding(start = 18.dp)){
                Canvas(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                        .align(CenterHorizontally)
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
                    color = Color(0xFF443741),
                )
                Text(
                    text = timerExtra,
                    style = MaterialTheme.typography.h5,
                    color = Color(0xFF443741),
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

            Text(
                text = player.name,
                style = MaterialTheme.typography.h2,
                color = Color(0xFF443741),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

private fun Button.getIcon() = when(this) {
    CONFIRM_MOVE -> Icons.Rounded.ThumbUp
    DISCARD_MOVE -> Icons.Rounded.Cancel
    ANALYZE -> Icons.Rounded.Biotech
    PASS -> Icons.Rounded.Stop
    RESIGN -> Icons.Outlined.Flag
    CHAT -> Icons.Filled.Forum
    NEXT_GAME -> Icons.Rounded.NextPlan
    UNDO -> Icons.Rounded.Undo
}

@Preview (showBackground = true)
@Composable
fun Preview() {
    OnlineGoTheme {
        GameScreen(state = GameState(
            position = Position(19, 19),
            loading = false,
            gameWidth = 19,
            gameHeight = 19,
            candidateMove = null,
            boardInteractive = false,
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
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
            )
        ))
    }
}

@Preview (showBackground = true)
@Composable
fun Preview1() {
    OnlineGoTheme {
        GameScreen(state = GameState(
            position = Position(19, 19),
            loading = false,
            gameWidth = 19,
            gameHeight = 19,
            candidateMove = null,
            boardInteractive = false,
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
                blackFirstLine = "04:26",
                blackSecondLine = "+ 3 × 01:00",
                blackPercentage = 15,
            )
        ))
    }
}