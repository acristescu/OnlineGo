package io.zenandroid.onlinego.ui.screens.learn

import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by alex on 05/11/2017.
 */
class LearnViewModel(
        private val tutorialsRepository: TutorialsRepository
) : ViewModel() {
    fun onAction(action: LearnAction) {
        when(action) {
            is LearnAction.TutorialGroupClicked -> _state.value = _state.value.copy(
                    expandedTutorialGroup = if (_state.value.expandedTutorialGroup == action.group) null else action.group
            )
        }
    }

    private val _state = MutableStateFlow(initialState())

    val state: StateFlow<LearnState> = _state

    private fun initialState(): LearnState {
        var state = LearnState(
                tutorialsRepository.getTutorialGroups()
        )
        for(group in state.tutorialGroups!!) {
            if(group.tutorials.find { !it.completed } != null) {
                state = state.copy(
                        expandedTutorialGroup = group
                )
                break
            }
        }
        return state

    }
}