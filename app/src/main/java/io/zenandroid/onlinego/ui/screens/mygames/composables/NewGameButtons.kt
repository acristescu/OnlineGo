package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
  onPlayOnline: () -> Unit,
  onCustomGame: () -> Unit,
  onPlayAgainstAI: () -> Unit,
  onFaceToFace: () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = modifier.fillMaxWidth()
  ) {
    NewGameButton(img = R.drawable.ic_person_filled, text = "Play\nOnline", onPlayOnline)
    NewGameButton(img = R.drawable.ic_tool, text = "Custom\nGame", onCustomGame)
    NewGameButton(img = R.drawable.ic_robot, text = "Play\nAgainst AI", onPlayAgainstAI)
    NewGameButton(img = Icons.Rounded.SportsBar, text = "Face\nto Face", onFaceToFace)
  }
}

@Composable
fun NewGameButton(img: ImageVector, text: String, onClick: () -> Unit) {
  NewGameButton(rememberVectorPainter(image = img), text, onClick)
}

@Composable
fun NewGameButton(@DrawableRes img: Int, text: String, onClick: () -> Unit) {
  NewGameButton(painterResource(img), text, onClick)
}

@Composable
fun NewGameButton(painter: Painter, text: String, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .clickable { onClick.invoke() }
      .padding(vertical = 8.dp)
  ) {
    Image(
      painter = painter,
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
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
    )
  }
}

@Preview
@Composable
private fun Preview() {
  OnlineGoTheme {
    NewGameButtonsRow(Modifier, {}, {}, {}, {})
  }
}