package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.ui.theme.salmon

@Composable
fun TutorialItem(percentage: Int, tutorial: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$percentage %",
            fontWeight = FontWeight.Black,
            color = salmon,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(24.dp)
        )
        Surface(
            color = salmon,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(start = 12.dp)
        ) {
            Row {
                Canvas(
                    modifier = Modifier
                        .size(25.dp, 50.dp)
                        .align(Alignment.CenterVertically)) {
                    drawArc(
                        color = Color.White,
                        alpha = .25f,
                        startAngle = 90f,
                        sweepAngle = -180f,
                        useCenter = false,
                        topLeft = Offset(-size.width, 0f),
                        size = Size(size.width * 2, size.height),
                        style = Stroke(width = 24.dp.value)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = 90f,
                        sweepAngle = -180f * .73f,
                        useCenter = false,
                        topLeft = Offset(-size.width, 0f),
                        size = Size(size.width * 2, size.height),
                        style = Stroke(
                            width = 24.dp.value,
                            cap = StrokeCap.Round
                        )
                    )
                }
                Column {
                    Text(
                        text = "Learn to play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 20.dp)
                    )
                    Text(
                        text = tutorial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 18.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    TutorialItem(percentage = 73, tutorial = "Basics > The rules")
}