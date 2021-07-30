package io.zenandroid.onlinego.ui.screens.learn

import io.zenandroid.onlinego.data.model.local.TutorialGroup

data class LearnState(
        val tutorialGroups: List<TutorialGroup>? = null,
        val expandedTutorialGroup: TutorialGroup? = null,
        val completedTutorialsNames: Set<String>? = null
)