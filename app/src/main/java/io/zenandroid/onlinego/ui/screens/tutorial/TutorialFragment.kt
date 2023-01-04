package io.zenandroid.onlinego.ui.screens.tutorial

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByFragment
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByFragment.BackArrowPressed
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel

const val TUTORIAL_NAME = "TUTORIAL_NAME"

class TutorialFragment : Fragment() {

    private val viewModel: TutorialViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by rememberStateWithLifecycle(viewModel.state)

                OnlineGoTheme {
                    TutorialScreen(state, ::acceptAction)
                }
            }
        }
    }

    private fun acceptAction(tutorialAction: TutorialAction) {
        if(tutorialAction is HandledByFragment) {
            when(tutorialAction) {
                BackArrowPressed -> requireActivity().onBackPressed()
            }
        } else {
            viewModel.acceptAction(tutorialAction as HandledByViewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadTutorial(arguments?.getString(TUTORIAL_NAME) ?: "")
    }
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun TutorialScreen(state: TutorialState, listener: (TutorialAction) -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
        // App bar
        TopAppBar(
                title = { Text(text = state.tutorial?.name ?: "") },
                elevation = 1.dp,
                navigationIcon = {
                    IconButton(onClick = { listener.invoke(BackArrowPressed) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                backgroundColor = MaterialTheme.colors.surface
        )

        state.step?.let {
            when(LocalConfiguration.current.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Description(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                    state = state
                            )
                            ButtonBar(state, listener)
                        }

                        Board(state, listener)
                    }
                }
                else -> {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    ) {
                        Description(state = state)
                    }

                    Board(state, listener)
                    ButtonBar(state, listener)
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
private fun Board(state: TutorialState, listener: (TutorialAction) -> Unit) {
    Board(
            modifier = Modifier
                    .padding(12.dp)
                    .shadow(6.dp, MaterialTheme.shapes.large),
            boardWidth = state.position?.boardWidth ?: 9,
            boardHeight = state.position?.boardHeight ?: 9,
            boardTheme = BoardTheme.WOOD,
            position = state.position,
            removedStones = state.removedStones,
            candidateMove = state.hoveredCell,
            candidateMoveType = StoneType.BLACK,
            onTapMove = { if (state.boardInteractive) listener(BoardCellHovered(it)) },
            onTapUp = { if (state.boardInteractive) listener(BoardCellTapped(it)) }
    )
}

@Composable
private fun Description(modifier: Modifier = Modifier, state: TutorialState) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(text = state.text ?: "",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun ButtonBar(state: TutorialState, listener: (TutorialAction) -> Unit) {
    Box {
        Row(horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp)) {
            if (state.retryButtonVisible) {
                OutlinedButton(onClick = { listener.invoke(RetryPressed) }, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Filled.Refresh, tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(16.dp), contentDescription = null)
                    Text(text = "RETRY", color = MaterialTheme.colors.onSurface, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (state.nextButtonVisible) {
                Button(onClick = { listener.invoke(NextPressed) }, modifier = Modifier.weight(1f)) {
                    Text(text = "NEXT")
                }
            }
        }
        Snackbar(
                visible = state.node?.success == true,
                text = "Nice one!",
                button = "NEXT",
                icon = R.drawable.ic_check_circle,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colors.secondary,
                listener = { listener.invoke(NextPressed) }
        )

        Snackbar(
                visible = state.node?.failed == true,
                text = state.node?.message ?: "That's not quite right!",
                button = "RETRY",
                icon = R.drawable.ic_x_circle,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colors.secondary,
                listener = { listener.invoke(RetryPressed) }
        )
    }
}

@ExperimentalAnimationApi
@Composable
private fun Snackbar(visible: Boolean, text: String, button: String, @DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, listener: () -> Unit) {
    AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = modifier,
    ) {
        Surface(
                elevation = 4.dp,
                border = BorderStroke(width = .5.dp, Color.LightGray),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                        .clickable(onClick = {})
                        .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Image(painter = painterResource(icon),
                        modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 18.dp),
                    contentDescription = null
                )
                Text(text = text,
                        modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                )
                TextButton(onClick = listener, modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(all = 4.dp)) {
                    Text(button, color = tint, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        TutorialScreen(TutorialState(), {})
    }
}