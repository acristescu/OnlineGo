package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.runtime.Composable
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual val notificationPermissionRequired: Boolean = true

@Composable
internal actual fun rememberNotificationPermissionRequest(onAnswered: () -> Unit): () -> Unit = {
  UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
  ) { _, _ ->
    dispatch_async(dispatch_get_main_queue()) { onAnswered() }
  }
}
