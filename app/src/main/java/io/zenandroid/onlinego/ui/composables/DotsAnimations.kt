package io.zenandroid.onlinego.ui.composables
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// https://gist.github.com/EugeneTheDev/a27664cb7e7899f964348b05883cbccd

@Composable
fun DotsPulsing(dotSize: Dp = 24.dp, delayUnit: Int = 300, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {

    @Composable
    fun Dot(
        scale: Float
    ) = Spacer(
        Modifier
            .size(dotSize)
            .scale(scale)
            .background(
                color = color,
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animateScaleWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 4
                0f at delay with LinearEasing
                1f at delay + delayUnit with LinearEasing
                0f at delay + delayUnit * 2
            }
        )
    )

    val scale1 by animateScaleWithDelay(0)
    val scale2 by animateScaleWithDelay(delayUnit)
    val scale3 by animateScaleWithDelay(delayUnit * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        val spaceSize = 2.dp

        Dot(scale1)
        Spacer(Modifier.width(spaceSize))
        Dot(scale2)
        Spacer(Modifier.width(spaceSize))
        Dot(scale3)
    }
}

@Composable
fun DotsElastic(dotSize: Dp = 24.dp, delayUnit: Int = 300, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val minScale = 0.6f

    @Composable
    fun Dot(
        scale: Float
    ) = Spacer(
        Modifier
            .size(dotSize)
            .scale(scaleX = minScale, scaleY = scale)
            .background(
                color = color,
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animateScaleWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = minScale,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 4
                minScale at delay with LinearEasing
                1f at delay + delayUnit with LinearEasing
                minScale at delay + delayUnit * 2
            }
        )
    )

    val scale1 by animateScaleWithDelay(0)
    val scale2 by animateScaleWithDelay(delayUnit)
    val scale3 by animateScaleWithDelay(delayUnit * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        val spaceSize = 2.dp

        Dot(scale1)
        Spacer(Modifier.width(spaceSize))
        Dot(scale2)
        Spacer(Modifier.width(spaceSize))
        Dot(scale3)
    }
}

@Composable
fun DotsFlashing(dotSize: Dp = 24.dp, delayUnit: Int = 300, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val minAlpha = 0.1f

    @Composable
    fun Dot(
        alpha: Float
    ) = Spacer(
        Modifier
            .size(dotSize)
            .alpha(alpha)
            .background(
                color = color,
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animateAlphaWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = minAlpha,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 4
                minAlpha at delay with LinearEasing
                1f at delay + delayUnit with LinearEasing
                minAlpha at delay + delayUnit * 2
            }
        )
    )

    val alpha1 by animateAlphaWithDelay(0)
    val alpha2 by animateAlphaWithDelay(delayUnit)
    val alpha3 by animateAlphaWithDelay(delayUnit * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        val spaceSize = 2.dp

        Dot(alpha1)
        Spacer(Modifier.width(spaceSize))
        Dot(alpha2)
        Spacer(Modifier.width(spaceSize))
        Dot(alpha3)
    }
}

@Composable
fun DotsTyping(dotSize: Dp = 24.dp, delayUnit: Int = 300, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val maxOffset = 10f

    @Composable
    fun Dot(
        offset: Float
    ) = Spacer(
        Modifier
            .size(dotSize)
            .offset(y = -offset.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animateOffsetWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 4
                0f at delay with LinearEasing
                maxOffset at delay + delayUnit with LinearEasing
                0f at delay + delayUnit * 2
            }
        )
    )

    val offset1 by animateOffsetWithDelay(0)
    val offset2 by animateOffsetWithDelay(delayUnit)
    val offset3 by animateOffsetWithDelay(delayUnit * 2)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.padding(top = maxOffset.dp)
    ) {
        val spaceSize = 2.dp

        Dot(offset1)
        Spacer(Modifier.width(spaceSize))
        Dot(offset2)
        Spacer(Modifier.width(spaceSize))
        Dot(offset3)
    }
}

@Composable
fun DotsCollision(dotSize: Dp = 24.dp, delayUnit: Int = 300, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    val maxOffset = 30f
    val delayUnit = 500 // it's better to use longer delay for this animation

    @Composable
    fun Dot(
        offset: Float
    ) = Spacer(
        Modifier
            .size(dotSize)
            .offset(x = offset.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    val offsetLeft by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 3
                0f at 0 with LinearEasing
                -maxOffset at delayUnit / 2 with LinearEasing
                0f at delayUnit
            }
        )
    )
    val offsetRight by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 3
                0f at delayUnit with LinearEasing
                maxOffset at delayUnit + delayUnit / 2 with LinearEasing
                0f at delayUnit * 2
            }
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = maxOffset.dp)
    ) {
        val spaceSize = 2.dp

        Dot(offsetLeft)
        Spacer(Modifier.width(spaceSize))
        Dot(0f)
        Spacer(Modifier.width(spaceSize))
        Dot(offsetRight)
    }
}


@Preview(showBackground = true)
@Composable
fun DotsPreview() = MaterialTheme {
    Column(modifier = Modifier.padding(4.dp)) {
        val spaceSize = 16.dp

        Text(
            text = "Dots pulsing",
            style = MaterialTheme.typography.titleSmall
        )
        DotsPulsing()

        Spacer(Modifier.height(spaceSize))

        Text(
            text = "Dots elastic",
            style = MaterialTheme.typography.titleSmall
        )
        DotsElastic()

        Spacer(Modifier.height(spaceSize))

        Text(
            text = "Dots flashing",
            style = MaterialTheme.typography.titleSmall
        )
        DotsFlashing()

        Spacer(Modifier.height(spaceSize))

        Text(
            text = "Dots typing",
            style = MaterialTheme.typography.titleSmall
        )
        DotsTyping()

        Spacer(Modifier.height(spaceSize))

        Text(
            text = "Dots collision",
            style = MaterialTheme.typography.titleSmall
        )
        DotsCollision()
    }
}