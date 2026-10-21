package io.zenandroid.onlinego.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ColumnScope.ExtraStatusField(text: String?, modifier: Modifier = Modifier) {
  AnimatedVisibility(visible = text != null) {
    Text(
      text = text ?: "",
      style = MaterialTheme.typography.titleMedium,
      color = Color.White,
      textAlign = TextAlign.Center,
      modifier = modifier
    )
  }
}
