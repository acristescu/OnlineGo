package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
internal actual fun rememberOpenNotificationSettings(): () -> Unit = {
  NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
    UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
  }
}
