package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme


@Composable
fun ChatIndicator(chatCount: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp)) {
        Image(
            painter = painterResource(id = R.drawable.ic_chat_bubble),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = chatCount.toString(),
            color = MaterialTheme.colorScheme.surface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(18.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                .align(Alignment.TopEnd)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        ChatIndicator(chatCount = 8)
    }
}