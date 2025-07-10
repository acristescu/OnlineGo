package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun NewGameButtonsRow(
  modifier: Modifier = Modifier,
  playOnlineEnabled: Boolean,
  customGameEnabled: Boolean,
  onPlayOnline: () -> Unit,
  onCustomGame: () -> Unit,
  onPlayAgainstAI: () -> Unit,
  onFaceToFace: () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = modifier.fillMaxWidth()
  ) {
    NewGameButton(
      painter = painterResource(R.drawable.ic_person_filled),
      text = "Play\nOnline",
      onClick = onPlayOnline,
      enabled = playOnlineEnabled
    )
    NewGameButton(
      painter = painterResource(R.drawable.ic_tool),
      text = "Custom\nGame",
      onClick = onCustomGame,
      enabled = customGameEnabled
    )
    NewGameButton(
      painter = painterResource(R.drawable.ic_robot),
      text = "Play\nAgainst AI",
      onClick = onPlayAgainstAI,
      enabled = true
    )
    NewGameButton(
      painter = rememberVectorPainter(Icons.Rounded.SportsBar),
      text = "Face\nto Face",
      onClick = onFaceToFace,
      enabled = true
    )
  }
}

@Composable
fun NewGameButton(painter: Painter, text: String, enabled: Boolean, onClick: () -> Unit) {
  val alpha = if (enabled) 1f else 0.3f
  Column(
    modifier = Modifier
      .clickable(enabled) { onClick.invoke() }
      .padding(vertical = 8.dp)
  ) {
    Image(
      painter = painter,
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = alpha), shape = CircleShape)
        .alpha(alpha)
        .padding(16.dp)
    )
    Text(
      text = text,
      textAlign = TextAlign.Center,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(top = 4.dp)
        .alpha(alpha)
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
  OnlineGoTheme {
    NewGameButtonsRow(Modifier, false, false, {}, {}, {}, {},)
  }
}