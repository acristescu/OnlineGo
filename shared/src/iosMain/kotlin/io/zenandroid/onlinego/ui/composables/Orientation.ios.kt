package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun isLandscape(): Boolean =
  LocalWindowInfo.current.containerSize.let { it.width > it.height }
