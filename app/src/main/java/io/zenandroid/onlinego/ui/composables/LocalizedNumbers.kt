package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun formatOneDecimal(value: Double): String =
  String.format(LocalConfiguration.current.locales[0], "%.1f", value)
