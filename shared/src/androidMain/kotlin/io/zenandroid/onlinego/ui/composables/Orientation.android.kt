package io.zenandroid.onlinego.ui.composables

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun isLandscape(): Boolean =
  LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
