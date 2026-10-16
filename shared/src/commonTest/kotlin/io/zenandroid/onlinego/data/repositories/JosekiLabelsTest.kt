package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.gamelogic.RulesManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JosekiLabelsTest {

  @Test
  fun customMarksBecomeBoldLabelsAndBoardMarks() {
    val position =
      extractLabelsFromDescription(JosekiPosition(description = "Play <A:Q16> or <B:R4>."))

    assertEquals("Play **A** or **B**.", position.description)
    assertEquals(
      listOf(
        Mark(RulesManager.coordinateToCell("Q16"), "A", PlayCategory.LABEL),
        Mark(RulesManager.coordinateToCell("R4"), "B", PlayCategory.LABEL),
      ),
      position.labels,
    )
  }

  @Test
  fun aLabelIsInsertedLiterally() {
    val position = extractLabelsFromDescription(JosekiPosition(description = "<$:Q16>"))

    assertEquals("**$**", position.description)
  }

  @Test
  fun headersGetTheirMissingSpace() {
    val position =
      extractLabelsFromDescription(JosekiPosition(description = "#Title\n##Sub\n# Fine"))

    assertEquals("# Title\n## Sub\n# Fine", position.description)
  }

  @Test
  fun positionReferencesBecomeLinks() {
    val position = extractLabelsFromDescription(JosekiPosition(description = "See <position: 123>"))

    assertEquals("See [Position 123](123)", position.description)
  }

  @Test
  fun aPositionWithoutDescriptionIsLeftAlone() {
    val position = extractLabelsFromDescription(JosekiPosition())

    assertNull(position.description)
    assertNull(position.labels)
  }
}
