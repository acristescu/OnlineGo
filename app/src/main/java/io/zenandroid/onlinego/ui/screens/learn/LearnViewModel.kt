package io.zenandroid.onlinego.ui.screens.learn

import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.utils.addToDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by alex on 05/11/2017.
 */
class LearnViewModel(
        private val tutorialsRepository: TutorialsRepository
) : ViewModel() {
    fun onAction(action: LearnAction) {
        when(action) {
            is LearnAction.TutorialGroupClicked -> _state.update {
              it.copy(
                expandedTutorialGroup = if (it.expandedTutorialGroup == action.group) null else action.group
              )
            }
            else -> {}
        }
    }

    private val disposables = CompositeDisposable()
    private val _state = MutableStateFlow(initialState())

    val state: StateFlow<LearnState> = _state

    private fun initialState(): LearnState {
        tutorialsRepository.completedTutorialsNames
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::onCompletedTutorialsChanged)
                .addToDisposable(disposables)

        return LearnState(
                tutorialsRepository.getTutorialGroups()
        )
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