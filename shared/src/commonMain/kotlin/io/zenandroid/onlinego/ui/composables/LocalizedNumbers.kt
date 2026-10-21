package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun formatOneDecimal(value: Double): String = formatOneDecimal(value, decimalSeparator())

internal fun formatOneDecimal(value: Double, decimalSeparator: Char): String {
  val tenths = (abs(value) * 10).roundToLong()
  val sign = if (value < 0 && tenths > 0) "-" else ""
  return "$sign${tenths / 10}$decimalSeparator${tenths % 10}"
}

@Composable
internal expect fun decimalSeparator(): Char
