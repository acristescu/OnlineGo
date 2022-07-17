package io.zenandroid.onlinego.ui.screens.game.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.screens.game.PlayerData
import io.zenandroid.onlinego.ui.theme.brown
import io.zenandroid.onlinego.utils.processGravatarURL

@Composable
fun PlayerCard(player: PlayerData?, timerMain: String, timerExtra: String, timerPercent: Int, timerFaded: Boolean, modifier: Modifier = Modifier) {
    val alpha = if(timerFaded) .6f else 1f
    player?.let {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            val maxSize = 84.dp
            val minSize = 64.dp
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 18.dp)
                    .sizeIn(minHeight = minSize, maxHeight = maxSize, minWidth = 64.dp)
                    .fillMaxHeight(1f)
            ){
                val color = MaterialTheme.colors.onSurface
                Canvas(
                    modifier = Modifier
                        .size(0.dp)
                        .weight(1f)
                        .aspectRatio(1f, true)
                        .align(Alignment.CenterHorizontally)
                        .alpha(alpha)
                ) {
                    val radius = (this.size.width - 2.dp.toPx()) / 2
                    drawCircle(
                        color = color,
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = color,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()),
                        startAngle = -90f,
                        sweepAngle = -360f * timerPercent / 100f,
                        useCenter = true,
                    )
                }
                Text(
                    text = timerMain,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface.copy(alpha = alpha),
                )
                Text(
                    text = timerExtra,
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onSurface.copy(alpha = alpha),
                )
            }
            Box(modifier = Modifier
                .padding(start = 16.dp)
                .sizeIn(minHeight = minSize, maxHeight = maxSize)
                .aspectRatio(1f, true)
            ) {
                val shape = CircleShape
//                val shape = RoundedCornerShape(14.dp)
                Image(
                    painter = rememberImagePainter(
                        data = processGravatarURL(player.iconURL, LocalDensity.current.run { 60.dp.roundToPx() }),
                        builder = {
                            placeholder(R.mipmap.placeholder)
                            error(R.mipmap.placeholder)
                        }
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp, end = 4.dp)
                        .shadow(2.dp, shape)
                        .clip(shape)
                )
                Box(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp)
                ) {
                    val shield =
                        if (player.color == StoneType.BLACK) R.drawable.black_shield else R.drawable.white_shield
                    Image(
                        painter = painterResource(id = shield),
                        contentDescription = null,
                    )
                    Text(
                        text = player.rank,
                        style = MaterialTheme.typography.h5,
                        color = if (player.color == StoneType.WHITE) brown else Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 5.dp, end = 1.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 16.dp)
                .sizeIn(minHeight = minSize, maxHeight = maxSize)
                .fillMaxHeight(1f)
            ) {
                Text(
                    text = player.name + "  " + player.flagCode,
                    style = MaterialTheme.typography.h2,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                )
                Text(
                    text = player.details,
                    style = MaterialTheme.typography.h5.copy(fontSize = 10.sp),
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
