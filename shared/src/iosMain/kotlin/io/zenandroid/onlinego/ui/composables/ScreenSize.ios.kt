package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize

@Composable
actual fun screenSize(): DpSize =
  with(LocalDensity.current) { LocalWindowInfo.current.containerSize.toSize().toDpSize() }
