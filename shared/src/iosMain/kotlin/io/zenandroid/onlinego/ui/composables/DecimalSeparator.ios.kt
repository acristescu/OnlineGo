package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import platform.Foundation.decimalSeparator

@Composable
internal actual fun decimalSeparator(): Char = appLocale().decimalSeparator.first()
