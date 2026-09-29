@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.local

import androidx.annotation.DrawableRes
import io.zenandroid.onlinego.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.UseSerializers

/**
 * [name] is a string resource name (e.g. "tutorial_basics_title"), not display text - it's
 * resolved at render time (see resolveTutorialText in ui/composables) so it stays locale-invariant
 * and doubles as this tutorial's stable identity (routing, lookup, completion tracking).
 */
@Serializable
data class Tutorial(
  val name: String,
  val steps: List<TutorialStep>
)

@Serializable
data class TutorialGroup(
  val name: String,
  val icon: TutorialIcon = TutorialIcon.GENERIC,
  val tutorials: List<Tutorial>
)

@Serializable
sealed class TutorialStep {
  @Serializable
  @SerialName("Interactive")
  data class Interactive(
    val name: String,
    val size: Int,
    val init: String,
    val text: String,
    val branches: List<Node>

  ) : TutorialStep()

  @Serializable
  @SerialName("Lesson")
  data class Lesson(
    val name: String,
    val size: Int,
    val pages: List<Page>
  ) : TutorialStep()

  @Serializable
  @SerialName("Game")
  data class GameExample(
    val name: String,
    val size: Int,
    val text: String,
    val sgf: String
  ) : TutorialStep()
}

@Serializable
data class Node(
  val move: String,
  val reply: String? = null,
  val message: String? = null,
  val success: Boolean = false,
  val failed: Boolean = false,
  val branches: List<Node>? = null
)

@Serializable
data class Page(
  val text: String,
  val position: String,
  val marks: String? = null,
  val areas: String? = null
)

@Serializable
enum class TutorialIcon(@DrawableRes val resId: Int) {
  BEGINNER(R.drawable.ic_tutorial_beginner),
  INTERMEDIATE(R.drawable.ic_tutorial_intermediate),
  ADVANCED(R.drawable.ic_tutorial_advanced),
  GENERIC(R.drawable.ic_learn)
}