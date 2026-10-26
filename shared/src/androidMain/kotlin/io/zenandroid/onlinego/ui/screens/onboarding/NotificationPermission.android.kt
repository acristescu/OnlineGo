package io.zenandroid.onlinego.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

internal actual val notificationPermissionRequired: Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
internal actual fun rememberNotificationPermissionRequest(onAnswered: () -> Unit): () -> Unit {
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { onAnswered() }
  return {
    if (notificationPermissionRequired) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
