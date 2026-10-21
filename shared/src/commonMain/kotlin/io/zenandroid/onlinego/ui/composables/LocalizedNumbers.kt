package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun formatDecimal(value: Double, fractionDigits: Int = 1): String =
  formatDecimal(value, fractionDigits, decimalSeparator())

internal fun formatDecimal(value: Double, fractionDigits: Int, decimalSeparator: Char): String {
  require(fractionDigits > 0)
  if (!value.isFinite()) return value.toString()
  val unit = 10.0.pow(fractionDigits).toLong()
  val scaled = (abs(value) * unit).roundToLong()
  val sign = if (value < 0 && scaled > 0) "-" else ""
  val fraction = (scaled % unit).toString().padStart(fractionDigits, '0')
  return "$sign${scaled / unit}$decimalSeparator$fraction"
}

@Composable
internal expect fun decimalSeparator(): Char
