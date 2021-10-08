package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL

@Composable
fun HomeScreenHeader(
    image: String? = null,
    mainText: String,
    subText: String? = null
) {
    Row(modifier = Modifier.padding(20.dp)) {
        val imageSize = 56.dp
        Image(
            painter = rememberImagePainter(
                data = processGravatarURL(image, LocalDensity.current.run { imageSize.roundToPx() } ),
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.ic_person_filled)
                }
            ),
            contentDescription = "Icon",
            modifier = Modifier
                .size(56.dp)
//                .border(color = Color.Black, width = 1.dp, shape = CircleShape)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.padding(start = 30.dp)) {
            Text(
                text = mainText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface,
            )
            subText?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colors.onSurface,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnlineGoTheme(darkTheme = true) {
        HomeScreenHeader(
            mainText = "Hi Alex,",
            subText = "It's your turn in 4 games."
        )
    }
}