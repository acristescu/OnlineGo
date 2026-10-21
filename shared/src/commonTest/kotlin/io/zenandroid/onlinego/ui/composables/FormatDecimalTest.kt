package io.zenandroid.onlinego.ui.composables

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDecimalTest {

  @Test
  fun roundsWithTheGivenSeparator() {
    assertEquals("61.5", formatDecimal(61.54, 1, '.'))
    assertEquals("61,6", formatDecimal(61.56, 1, ','))
    assertEquals("10,0", formatDecimal(9.96, 1, ','))
    assertEquals("0.0", formatDecimal(0.0, 1, '.'))
    assertEquals("0.4", formatDecimal(0.4, 1, '.'))
  }

  @Test
  fun padsTheFractionToTheRequestedDigits() {
    assertEquals("45.07", formatDecimal(45.0712, 2, '.'))
    assertEquals("100,00", formatDecimal(99.999, 2, ','))
  }

  @Test
  fun keepsTheSignOnlyWhenTheRoundedValueIsNotZero() {
    assertEquals("-2.3", formatDecimal(-2.34, 1, '.'))
    assertEquals("0.0", formatDecimal(-0.04, 1, '.'))
  }

  @Test
  fun leavesNonFiniteValuesAsTheyPrint() {
    assertEquals("NaN", formatDecimal(Double.NaN, 2, '.'))
  }
}
