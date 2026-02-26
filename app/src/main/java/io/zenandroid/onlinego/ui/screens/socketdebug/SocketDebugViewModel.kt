package io.zenandroid.onlinego.ui.screens.socketdebug

import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.data.repositories.SocketDebugRepository
import io.zenandroid.onlinego.data.repositories.SocketEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SocketDebugViewModel(
  private val socketDebugRepository: SocketDebugRepository,
) : ViewModel() {

  val events = socketDebugRepository.events
  val connectionState = socketDebugRepository.connectionState

  private val _filter = MutableStateFlow<SocketEventType?>(null)
  val filter: StateFlow<SocketEventType?> = _filter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  fun setFilter(type: SocketEventType?) {
    _filter.value = type
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun clear() {
    socketDebugRepository.clear()
  }
}

