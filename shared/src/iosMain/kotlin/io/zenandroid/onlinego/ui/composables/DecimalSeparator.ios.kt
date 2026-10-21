package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.decimalSeparator

@Composable
internal actual fun decimalSeparator(): Char = NSLocale.currentLocale.decimalSeparator.first()
