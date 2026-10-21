package io.zenandroid.onlinego.ui.composables

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatOneDecimalTest {

  @Test
  fun roundsToOneDecimalWithTheGivenSeparator() {
    assertEquals("61.5", formatOneDecimal(61.54, '.'))
    assertEquals("61,6", formatOneDecimal(61.56, ','))
    assertEquals("10,0", formatOneDecimal(9.96, ','))
    assertEquals("0.0", formatOneDecimal(0.0, '.'))
    assertEquals("0.4", formatOneDecimal(0.4, '.'))
  }

  @Test
  fun keepsTheSignOnlyWhenTheRoundedValueIsNotZero() {
    assertEquals("-2.3", formatOneDecimal(-2.34, '.'))
    assertEquals("0.0", formatOneDecimal(-0.04, '.'))
  }
}
