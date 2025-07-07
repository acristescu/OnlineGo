package io.zenandroid.onlinego.ui.screens.newchallenge

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.repositories.BotsRepository
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class SelectOpponentViewModel(
  botsRepository: BotsRepository,
  private val playersRepository: PlayersRepository
) : ViewModel() {
  val state = MutableStateFlow(SelectOpponentState(
    bots = botsRepository
      .bots
      .sortedBy { it.rating }
  ))

  private var currentSearch: Job? = null

  init {
    viewModelScope.launch {
      val recentOpponents = playersRepository.getRecentOpponents()
      state.update {
        it.copy(recentOpponents = recentOpponents)
      }
    }
  }

  fun onEvent(event: Event) {
    when (event) {
      is Event.OpponentSelected -> {}

      is Event.TabSelected -> {
        state.update {
          it.copy(selectedTab = event.tab)
        }
      }

      is Event.SearchTermChanged -> {
        state.update {
          it.copy(searchTerm = event.query)
        }
        currentSearch?.cancel()
        currentSearch = viewModelScope.launch {
          try {
            delay(100)
            val searchResults = playersRepository.searchPlayers(event.query)
            state.update {
              it.copy(searchResults = searchResults)
            }
          } catch(e: CancellationException) {
            throw e
          } catch (e: Exception) {
            state.update {
              it.copy(error = e.message ?: "Unknown error" )
            }
          }
        }
      }
    }
  }
}

@Immutable
data class SelectOpponentState(
  val selectedTab: Tab = Tab.BOT,
  val bots: List<Player> = emptyList(),
  val recentOpponents: List<Player> = emptyList(),
  val searchTerm: String = "",
  val searchResults: List<Player> = emptyList(),
  val error: String? = null
)

enum class Tab {
  BOT,
  RECENT,
  SEARCH,
}

sealed interface Event {
  data class TabSelected(val tab: Tab) : Event
  data class OpponentSelected(val opponent: Player) : Event
  data class SearchTermChanged(val query: String) : Event
}