package io.zenandroid.onlinego.ui.composables

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun formatMediumDate(epochSeconds: Long): String =
  DateUtils.formatDateTime(
    LocalContext.current,
    epochSeconds * 1000,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
  )
