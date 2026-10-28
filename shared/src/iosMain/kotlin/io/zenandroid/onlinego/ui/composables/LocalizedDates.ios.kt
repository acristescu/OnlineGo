package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.dateWithTimeIntervalSince1970

@Composable
internal actual fun formatMediumDate(epochSeconds: Long): String =
  NSDateFormatter().apply {
    locale = appLocale()
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterNoStyle
  }.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble()))
