package io.zenandroid.onlinego.ui.screens.tutorial

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableAmbient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.lifecycle.asLiveData
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByFragment
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByFragment.BackArrowPressed
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.HandledByViewModel.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.ui.views.BoardView
import org.koin.androidx.viewmodel.ext.android.viewModel

const val TUTORIAL_NAME = "TUTORIAL_NAME"

class TutorialFragment : Fragment() {

    companion object {
        fun newInstance(tutorialName: String) = TutorialFragment().apply {
            arguments = bundleOf(TUTORIAL_NAME to tutorialName)
        }
    }

    private val viewModel: TutorialViewModel by viewModel()

    @ExperimentalAnimationApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.asLiveData().observeAsState()

                state?.let {
                    OnlineGoTheme {
                        TutorialScreen(it, ::acceptAction)
                    }
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
                        Icon(imageVector = Icons.Default.ArrowBack)
                    }
                },
                backgroundColor = MaterialTheme.colors.surface
        )

        state.step?.let {
            // Text description area
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                            .weight(1f)
//                            .background(MaterialTheme.colors.surface)
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                ScrollableColumn {
                    Text(text = state.text ?: "",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2,
                            fontSize = TextUnit(18),
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Board
            val context = AmbientContext.current
            val boardView = remember {
                BoardView(context).apply {
                    onTapMove = { listener(BoardCellHovered(it)) }
                    onTapUp = { listener(BoardCellTapped(it)) }
                    drawCoordinates = true
                    drawMarks = true
                    drawLastMove = true
                }
            }
            AndroidView({ boardView },
                    modifier = Modifier
                            .padding(12.dp)
                            .shadow(6.dp, MaterialTheme.shapes.large)
            ) { view ->
                view.apply {
                    boardSize = 9
                    position = state.position
                    isInteractive = state.boardInteractive
                    showCandidateMove(state.hoveredCell, StoneType.BLACK)
                }
            }

            // Bottom buttons
            Box {
                Row(horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)) {
                    if(state.retryButtonVisible) {
                        OutlinedButton(onClick = { listener.invoke(RetryPressed) }, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Filled.Refresh, tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(16.dp))
                            Text(text = "RETRY", color = MaterialTheme.colors.onSurface, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if(state.nextButtonVisible) {
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
    }
}

@ExperimentalAnimationApi
@Composable
private fun Snackbar(visible: Boolean, text: String, button: String, @DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, listener: () -> Unit) {
    ColumnScope.AnimatedVisibility(
            visible = visible,
            initiallyVisible = false,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = modifier,
    ) {
        Surface(
                elevation = 4.dp,
                border = BorderStroke(width = .5.dp, Color.LightGray),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                        .clickable(onClick = {}, indication = null)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Image(painter = painterResource(icon),
                        modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 18.dp)
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

@ExperimentalAnimationApi
@Preview
@Composable
fun Preview() {
    OnlineGoTheme {
        TutorialScreen(TutorialState(), {})
    }
}