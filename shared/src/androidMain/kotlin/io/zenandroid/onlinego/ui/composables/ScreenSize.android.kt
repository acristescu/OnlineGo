package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
actual fun screenSize(): DpSize =
  LocalConfiguration.current.let { DpSize(it.screenWidthDp.dp, it.screenHeightDp.dp) }
