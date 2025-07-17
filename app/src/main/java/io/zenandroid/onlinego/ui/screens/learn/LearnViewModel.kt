package io.zenandroid.onlinego.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.utils.analyticsReportScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by alex on 05/11/2017.
 */
class LearnViewModel(
  private val tutorialsRepository: TutorialsRepository
) : ViewModel() {
  fun onAction(action: LearnAction) {
    when (action) {
      is LearnAction.TutorialGroupClicked -> _state.update {
        it.copy(
          expandedTutorialGroup = if (it.expandedTutorialGroup == action.group) null else action.group
        )
      }

      else -> {}
    }
  }

  private val disposables = CompositeDisposable()
  private val _state = MutableStateFlow(LearnState())

  val state: StateFlow<LearnState> = _state

  init {
    analyticsReportScreen("Learn")
    viewModelScope.launch(Dispatchers.IO) {
      val tutorialGroups = tutorialsRepository.getTutorialGroups()
      _state.update {
        it.copy(
          tutorialGroups = tutorialGroups,
          expandedTutorialGroup = tutorialGroups.firstOrNull()
        )
      }
      viewModelScope.launch {
        tutorialsRepository.completedTutorialsNames.collect {
          onCompletedTutorialsChanged(it)
        }
      }
    }
  }

  private fun onCompletedTutorialsChanged(completedNames: Set<String>) {
    var expandedTutorialGroup: TutorialGroup? = null
    for (group in _state.value.tutorialGroups!!) {
      if (group.tutorials.find { !completedNames.contains(it.name) } != null) {
        expandedTutorialGroup = group
        break
      }
    }
    _state.update {
      it.copy(
        completedTutorialsNames = completedNames,
        expandedTutorialGroup = expandedTutorialGroup
      )
    }
  }

  override fun onCleared() {
    disposables.clear()
  }
}