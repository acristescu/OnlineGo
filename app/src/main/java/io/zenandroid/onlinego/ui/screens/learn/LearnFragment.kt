package io.zenandroid.onlinego.ui.screens.learn

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.loadVectorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Created by alex on 05/11/2017.
 */
@ExperimentalAnimationApi
@ExperimentalMaterialApi
class LearnFragment : Fragment() {


    private val viewModel: LearnViewModel by viewModel()
    private val analytics = OnlineGoApplication.instance.analytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.asLiveData().observeAsState()

                state?.let {
                    OnlineGoTheme {
                        Screen(it, ::onAction)
                    }
                }
            }
        }
    }

    private fun onAction(action: LearnAction) {
        when(action) {
            LearnAction.JosekiExplorerClicked -> (activity as MainActivity).navigateToJosekiExplorer()
            is LearnAction.TutorialClicked -> (activity as MainActivity).navigateToTutorialScreen(action.tutorial)
            else -> viewModel.onAction(action)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)

        (activity as? MainActivity)?.apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            setLogoVisible(true)
            setChipsVisible(false)
            setChatButtonVisible(false)
            ensureNavigationVisible()
        }

    }
}

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Screen(state: LearnState, listener: (LearnAction) -> Unit) {
    ScrollableColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
        Section(title = "Tutorials") {
            state.tutorialGroups?.forEach {
                PrimaryRow(title = it.name, it.icon.resId) { listener(LearnAction.TutorialGroupClicked(it)) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(visible = state.expandedTutorialGroup == it, initiallyVisible = false) {
                        Column {
                            for (tutorial in it.tutorials) {
                                SecondaryRow(tutorial) {
                                    listener.invoke(LearnAction.TutorialClicked(tutorial))
                                }
                            }
                        }
                    }
                }
            }
        }
        Section(title = "Study") {
            PrimaryRow(title = "Joseki explorer", R.drawable.ic_learn) { listener(LearnAction.JosekiExplorerClicked) }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    SectionHeader(title)
    Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.h4,
            color = Color(0xFF9B9B9B),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun PrimaryRow(title: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    Row(modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Icon(painterResource(icon))
        Text(title, style = MaterialTheme.typography.body2, modifier = Modifier
                .padding(start = 24.dp)
                .align(Alignment.CenterVertically))
    }
}

@Composable
fun SecondaryRow(tutorial: Tutorial, onClick: () -> Unit) {
    Row(modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(start = 72.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if(tutorial.completed) {
            Image(painterResource(R.drawable.ic_checked_square), modifier = Modifier.align(Alignment.CenterVertically))
        } else {
            Image(painterResource(R.drawable.ic_unchecked_square), modifier = Modifier.align(Alignment.CenterVertically))
        }
        Text(tutorial.name, style = MaterialTheme.typography.body2, modifier = Modifier
                .padding(start = 24.dp)
                .align(Alignment.CenterVertically))
    }
}

@Preview(showBackground = true)
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun DefaultPreview() {
    OnlineGoTheme {
        Screen(LearnState(TutorialsRepository().getTutorialGroups()), {})
    }
}

