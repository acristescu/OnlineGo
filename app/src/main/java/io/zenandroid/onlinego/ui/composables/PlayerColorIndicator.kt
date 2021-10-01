package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme


@Composable
fun PlayerColorIndicator(color: StoneType, modifier: Modifier = Modifier) {
    val circleColor = if (color == StoneType.BLACK) Color.Black else Color.White
    Box(
        modifier = modifier
            .padding(top = 2.dp, start = 8.dp)
            .background(MaterialTheme.colors.onSurface, shape = CircleShape)
            .padding(all = 1.dp) // width of the line of the empty circle
            .background(color = circleColor, shape = CircleShape)
            .size(8.dp) // size of the middle circle
    )
}

@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        PlayerColorIndicator(color = StoneType.WHITE)
    }
}

@Composable
private fun Preview1() {
    OnlineGoTheme {
        PlayerColorIndicator(color = StoneType.BLACK)
    }
}