package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable

@Composable
expect fun formatMediumDate(epochSeconds: Long): String
