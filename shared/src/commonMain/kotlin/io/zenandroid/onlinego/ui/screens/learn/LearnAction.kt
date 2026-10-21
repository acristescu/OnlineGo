package io.zenandroid.onlinego.ui.screens.learn

import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialGroup

sealed class LearnAction {
    class TutorialClicked(val tutorial: Tutorial): LearnAction()
    class TutorialGroupClicked(val group: TutorialGroup) : LearnAction()
    object JosekiExplorerClicked: LearnAction()
    object PuzzlesClicked: LearnAction()
}