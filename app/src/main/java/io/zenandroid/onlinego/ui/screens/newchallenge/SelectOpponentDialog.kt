package io.zenandroid.onlinego.ui.screens.newchallenge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.pager.pagerTabIndicatorOffset
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.utils.PreviewBackground
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.compose.getViewModel

@ExperimentalFoundationApi
@Composable
fun SelectOpponentDialog(
  onDialogDismiss: (Player?) -> Unit
) {
  val viewModel = getViewModel<SelectOpponentViewModel>()

  val state by rememberStateWithLifecycle(stateFlow = viewModel.state)

  SelectOpponentDialogContent(
    state = state,
    onEvent = { event ->
      if (event is Event.OpponentSelected) {
        onDialogDismiss(event.opponent)
      } else {
        viewModel.onEvent(event)
      }
    },
    onDialogDismiss = { onDialogDismiss(null) },
  )

  BackHandler { onDialogDismiss(null) }
}

@ExperimentalFoundationApi
@Composable
private fun SelectOpponentDialogContent(
  state: SelectOpponentState,
  onEvent: (Event) -> Unit,
  onDialogDismiss: () -> Unit
) {
  Box(modifier = Modifier
    .fillMaxSize()
    .background(Color(0x88000000))
    .clickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null
    ) { onDialogDismiss() }
  ) {
    Surface(modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { }
      .fillMaxWidth()
      .fillMaxHeight()
      .align(Alignment.Center)
      .shadow(4.dp)
      .background(
        color = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(10.dp)
      )
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val pagerState = rememberPagerState(pageCount = { 3 })
        LaunchedEffect(key1 = state.selectedTab) {
          pagerState.animateScrollToPage(Tab.entries.indexOf(state.selectedTab))
        }
        TabRow(
          selectedTabIndex = Tab.entries.indexOf(state.selectedTab),
          backgroundColor = Color.Transparent,
          contentColor = MaterialTheme.colors.primary,
          indicator = {
            TabRowDefaults.Indicator(
              Modifier.pagerTabIndicatorOffset(
                pagerState = pagerState,
                tabPositions = it
              )
            )
          },
        ) {
          Tab(
            selected = state.selectedTab == Tab.BOT,
            onClick = { onEvent(Event.TabSelected(Tab.BOT)) },
            text = { Text(text = "Bot") },
            selectedContentColor = MaterialTheme.colors.primary,
            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          )
          Tab(
            selected = state.selectedTab == Tab.RECENT,
            onClick = { onEvent(Event.TabSelected(Tab.RECENT)) },
            text = { Text(text = "Recent") },
            selectedContentColor = MaterialTheme.colors.primary,
            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          )
          Tab(
            selected = state.selectedTab == Tab.SEARCH,
            onClick = { onEvent(Event.TabSelected(Tab.SEARCH)) },
            text = { Text(text = "Search") },
            selectedContentColor = MaterialTheme.colors.primary,
            unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          )
        }
        HorizontalPager(state = pagerState) { page ->
          when (page) {
            0 -> BotList(
              state = state,
              onOpponentSelected = { onEvent(Event.OpponentSelected(it)) }
            )

            1 -> RecentList(
              state = state,
              onOpponentSelected = { onEvent(Event.OpponentSelected(it)) }
            )
            2 -> Search()
          }
        }
      }
    }
  }
}

@Composable
private fun BotList(state: SelectOpponentState, onOpponentSelected: (Player) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "Online Bots",
      style = MaterialTheme.typography.h6,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    Text(
      text = "Online bots are AI programs run and maintained by members of the community at their expense. Playing against them requires an active internet connection.",
      style = MaterialTheme.typography.body1,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    LazyColumn {
      items(items = state.bots) {
        OpponentItem(
          opponent = it,
          modifier = Modifier
            .clickable { onOpponentSelected(it) }
            .padding(vertical = 8.dp)
        )
      }
    }
  }
}

@Composable
private fun OpponentItem(opponent: Player, modifier: Modifier = Modifier) {
  Row(modifier = modifier) {
    Image(
      painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
          .data(processGravatarURL(opponent.icon, LocalDensity.current.run { 48.dp.roundToPx() }))
          .crossfade(true)
          .placeholder(R.drawable.ic_person_filled_with_background)
          .error(R.mipmap.placeholder)
          .build()
      ),
      contentDescription = "Opponent icon",
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
    )
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.CenterVertically)
        .padding(start = 16.dp)
    ) {
      Text(
        text = opponent.username,
        fontSize = 16.sp,
      )
      Text(
        text = formatRank(egfToRank(opponent.rating), opponent.deviation),
        fontSize = 12.sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
      )
    }
  }
}

@Composable
private fun RecentList(state: SelectOpponentState, onOpponentSelected: (Player) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "Recent opponents",
      style = MaterialTheme.typography.h6,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    Text(
      text = "This is a selection of opponents (both bots and actual players) you've played against recently.",
      style = MaterialTheme.typography.body1,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    LazyColumn {
      items(items = state.recentOpponents) {
        OpponentItem(
          opponent = it,
          modifier = Modifier
            .clickable { onOpponentSelected(it) }
            .padding(vertical = 8.dp)
        )
      }
    }
  }

}

@Composable
private fun Search() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .padding(top = 16.dp)
  ) {
    Text(text = "Search")
  }
}


@Preview
@Composable
private fun SelectOpponentDialogPreview() {
  PreviewBackground {
    SelectOpponentDialogContent(
      state = SelectOpponentState(
        bots = listOf(
          Player(
            id = 0,
            username = "Bot",
            icon = "https://www.gravatar.com/avatar/00",
            rating = 1340.0,
            deviation = 0.0,
            acceptedStones = null,
            country = null,
            historicRating = null,
            ui_class = null,
          ),
          Player(
            id = 1,
            username = "Bot",
            icon = "https://www.gravatar.com/avatar/00",
            rating = 1340.0,
            deviation = 0.0,
            acceptedStones = null,
            country = null,
            historicRating = null,
            ui_class = null,
          ),
          Player(
            id = 2,
            username = "Bot",
            icon = "https://www.gravatar.com/avatar/00",
            rating = 1340.0,
            deviation = 0.0,
            acceptedStones = null,
            country = null,
            historicRating = null,
            ui_class = null,
          ),
        )
      ),
      onEvent = {},
      onDialogDismiss = {},
    )
  }
}