package io.zenandroid.onlinego.ui.screens.learn

import io.zenandroid.onlinego.data.model.local.TutorialGroup

//private val hardcodedTutorials = listOf(
//        Tutorial("The basics", listOf(Lesson("Basics 1", true),Lesson("Basics 2", true),Lesson("Basics 3", true),Lesson("Basics 4", true))),
//        Tutorial("Intermediate", listOf(Lesson("The ladder", true),Lesson("Capturing races"),Lesson("Snapback"))),
//        Tutorial("Advanced concepts", listOf(Lesson("Advanced 1"),Lesson("Advanced 2"),Lesson("Advanced 3"),Lesson("Advanced 4"))),
//        Tutorial("Game examples", listOf(Lesson("Game 1"),Lesson("Game 2"),Lesson("Game 3"),Lesson("Game 4"))),
//)

data class LearnState(
        val tutorialGroups: List<TutorialGroup>? = null,
        val expandedTutorialGroup: TutorialGroup? = null,
        val completedTutorialsNames: Set<String>? = null
)

//data class Tutorial(
//        val name: String,
//        val lessons: List<Lesson>
//)
//
//data class Lesson(
//        val name: String,
//        val completed: Boolean = false
//)

