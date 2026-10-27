package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalOnlineGoViewController =
  staticCompositionLocalOf<OnlineGoViewController?> { null }

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) {
  val host = LocalOnlineGoViewController.current
  SideEffect { host?.setDarkTheme(darkTheme) }
}
