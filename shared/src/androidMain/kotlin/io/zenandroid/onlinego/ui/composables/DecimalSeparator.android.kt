package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DecimalFormatSymbols

@Composable
internal actual fun decimalSeparator(): Char =
  DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator
