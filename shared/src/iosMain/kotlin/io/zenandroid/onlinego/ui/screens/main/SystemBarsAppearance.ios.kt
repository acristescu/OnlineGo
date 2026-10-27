package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.uikit.LocalUIViewController

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) {
  val host = LocalUIViewController.current.parentViewController as? OnlineGoViewController
  SideEffect { host?.setDarkTheme(darkTheme) }
}
