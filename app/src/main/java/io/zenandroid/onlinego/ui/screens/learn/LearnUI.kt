package io.zenandroid.onlinego.ui.screens.learn

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.ui.screens.mygames.composables.SenteCard
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LearnScreen(
  viewModel: LearnViewModel = koinViewModel(),
  onJosekiExplorer: () -> Unit,
  onPuzzles: () -> Unit,
  onTutorial: (tutorial: Tutorial) -> Unit,
) {
  val state by viewModel.state.collectAsState()

  LearnContent(state) { action ->
    when (action) {
      LearnAction.JosekiExplorerClicked -> onJosekiExplorer()
      LearnAction.PuzzlesClicked -> onPuzzles()
      is LearnAction.TutorialClicked -> onTutorial(action.tutorial)
      else -> viewModel.onAction(action)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnContent(state: LearnState, listener: (LearnAction) -> Unit) {
  Column {
    TopAppBar(
      title = {
        Text(
          text = "Learn",
          style = MaterialTheme.typography.headlineLarge.copy(fontWeight = Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      },
    )
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp)
    ) {
      Section(title = "Tutorials") {
        state.tutorialGroups?.forEach {
          PrimaryRow(
            title = it.name,
            it.icon.resId
          ) { listener(LearnAction.TutorialGroupClicked(it)) }
          Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = state.expandedTutorialGroup == it) {
              Column {
                for (tutorial in it.tutorials) {
                  val completed = state.completedTutorialsNames?.contains(tutorial.name) == true
                  SecondaryRow(tutorial, completed) {
                    listener.invoke(LearnAction.TutorialClicked(tutorial))
                  }
                }
              }
            }
          }
        }
      }
      Section(title = "Study") {
        PrimaryRow(
          title = "Joseki explorer",
          R.drawable.ic_branch
        ) { listener(LearnAction.JosekiExplorerClicked) }
        PrimaryRow(
          title = "Puzzles",
          R.drawable.ic_go_board
        ) { listener(LearnAction.PuzzlesClicked) }
      }
    }
  }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
  SectionHeader(title)
  SenteCard(
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
      content()
    }
  }
}

@Composable
private fun SectionHeader(text: String) {
  Text(
    text = text,
    fontSize = 12.sp,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Medium,
    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
  )
}

@Composable
fun PrimaryRow(title: String, @DrawableRes icon: Int, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .clickable(onClick = onClick)
      .fillMaxWidth()
      .padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
  ) {
    Icon(painter = painterResource(icon), contentDescription = null)
    Text(
      title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier
        .padding(start = 24.dp)
        .align(Alignment.CenterVertically)
    )
  }
}

@Composable
fun SecondaryRow(tutorial: Tutorial, completed: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .clickable(onClick = onClick)
      .fillMaxWidth()
      .padding(start = 72.dp, top = 8.dp, bottom = 8.dp)
  ) {
    if (completed) {
      Image(
        painter = painterResource(R.drawable.ic_checked_square), modifier = Modifier.align(
          Alignment.CenterVertically
        ), contentDescription = null
      )
    } else {
      Image(
        painter = painterResource(R.drawable.ic_unchecked_square), modifier = Modifier.align(
          Alignment.CenterVertically
        ), contentDescription = null
      )
    }
    Text(
      tutorial.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier
        .padding(start = 24.dp)
        .align(Alignment.CenterVertically)
    )
  }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  OnlineGoTheme {
    LearnContent(LearnState(TutorialsRepository().getTutorialGroups()), {})
  }
}

