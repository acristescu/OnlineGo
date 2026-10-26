package io.zenandroid.onlinego.ui.composables

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberOpenNotificationSettings(): () -> Unit {
  val context = LocalContext.current
  return remember(context) {
    {
      val intent = Intent("android.settings.APP_NOTIFICATION_SETTINGS")
        .putExtra("app_package", context.packageName)
        .putExtra("app_uid", context.applicationInfo.uid)
        .putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
      context.startActivity(intent)
    }
  }
}
