package io.zenandroid.onlinego.ui.screens.newchallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.repositories.BotsRepository
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.concurrent.Immutable

class SelectOpponentViewModel(
  botsRepository: BotsRepository,
  private val playersRepository: PlayersRepository
) : ViewModel() {
  val state = MutableStateFlow(SelectOpponentState())

  init {
    state.value = state.value.copy(
      bots = botsRepository
        .bots
        .sortedBy { it.rating }
    )

    viewModelScope.launch {
      val recentOpponents = playersRepository.getRecentOpponents()
      withContext(Dispatchers.Main) {
        state.value = state.value.copy(
          recentOpponents = recentOpponents
        )
      }
    }
  }

  fun onEvent(event: Event) {
    when (event) {
      is Event.OpponentSelected -> {
        state.value = state.value.copy(

        )
      }

      is Event.TabSelected -> {
        state.value = state.value.copy(
          selectedTab = event.tab
        )
      }
    }
  }
}

@Immutable
data class SelectOpponentState(
  val selectedTab: Tab = Tab.BOT,
  val bots: List<Player> = emptyList(),
  val recentOpponents: List<Player> = emptyList(),
)

enum class Tab {
  BOT,
  RECENT,
  SEARCH,
}

sealed interface Event {
  data class TabSelected(val tab: Tab) : Event
  data class OpponentSelected(val opponent: Player) : Event
}