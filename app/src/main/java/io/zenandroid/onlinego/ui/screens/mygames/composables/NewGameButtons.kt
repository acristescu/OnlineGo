package io.zenandroid.onlinego.ui.screens.mygames.composables

import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsBar
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
import io.zenandroid.onlinego.ui.screens.mygames.Action
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun NewGameButtonsRow(modifier: Modifier = Modifier, onAction: (Action) -> Unit) {
    Row (
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth()
    ) {
        NewGameButton(img = R.drawable.ic_person_filled, text = "Play\nOnline") { onAction(Action.PlayOnline) }
        NewGameButton(img = R.drawable.ic_tool, text = "Custom\nGame") { onAction(Action.CustomGame) }
        NewGameButton(img = R.drawable.ic_robot, text = "Play\nAgainst AI") { onAction(Action.PlayAgainstAI) }
        NewGameButton(img = Icons.Rounded.SportsBar, text = "Face\nto Face") { onAction(Action.FaceToFace) }
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
    Column(modifier = Modifier
        .clickable { onClick.invoke() }
        .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                .padding(16.dp)
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground,
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
        NewGameButtonsRow(onAction = {})
    }
}