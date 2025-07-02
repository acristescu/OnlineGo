package io.zenandroid.onlinego.data.model.local

import androidx.annotation.DrawableRes
import io.zenandroid.onlinego.R

data class Tutorial(
  val name: String,
  val steps: List<TutorialStep>
)

data class TutorialGroup(
  val name: String,
  val icon: TutorialIcon = TutorialIcon.GENERIC,
  val tutorials: List<Tutorial>
)

sealed class TutorialStep(val type: String) {
  data class Interactive(
    val name: String,
    val size: Int,
    val init: String,
    val text: String,
    val branches: List<Node>

  ) : TutorialStep("Interactive")

  data class Lesson(
    val name: String,
    val size: Int,
    val pages: List<Page>
  ) : TutorialStep("Lesson")

  data class GameExample(
    val name: String,
    val size: Int,
    val text: String,
    val sgf: String
  ) : TutorialStep("Game")
}

data class Node(
  val move: String,
  val reply: String? = null,
  val message: String? = null,
  val success: Boolean = false,
  val failed: Boolean = false,
  val branches: List<Node>? = null
)

data class Page(
  val text: String,
  val position: String,
  val marks: String? = null,
  val areas: String? = null
)

enum class TutorialIcon(@DrawableRes val resId: Int) {
  BEGINNER(R.drawable.ic_tutorial_beginner),
  INTERMEDIATE(R.drawable.ic_tutorial_intermediate),
  ADVANCED(R.drawable.ic_tutorial_advanced),
  GENERIC(R.drawable.ic_learn)
}