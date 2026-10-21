package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun bottomBarContentPadding(): Dp =
  WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 56.dp + 32.dp
