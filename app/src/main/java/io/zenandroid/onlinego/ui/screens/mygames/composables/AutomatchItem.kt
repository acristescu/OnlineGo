package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.SizeSpeedOption
import io.zenandroid.onlinego.ui.screens.mygames.Action
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun AutomatchItem(automatch: OGSAutomatch, onAction: (Action) -> Unit) {
    Surface (
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column (modifier = Modifier.padding(horizontal = 16.dp)){
            Text(
                text = "Searching for a game",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onAction(Action.AutomatchCancelled(automatch)) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        AutomatchItem(
            automatch = OGSAutomatch(
                uuid = "aaa",
                game_id = null,
                size_speed_options = listOf(
                    SizeSpeedOption("9x9", "blitz"),
                )
            ),
            onAction = {}
        )
    }
}