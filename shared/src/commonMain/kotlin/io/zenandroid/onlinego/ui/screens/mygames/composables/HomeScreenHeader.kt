package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.ic_person_filled_with_background
import io.zenandroid.onlinego.resources.mygames_header_icon_content_description
import io.zenandroid.onlinego.resources.mygames_offline_content_description
import io.zenandroid.onlinego.resources.mygames_connecting
import io.zenandroid.onlinego.resources.mygames_reconnecting
import io.zenandroid.onlinego.resources.mygames_refreshing_games
import io.zenandroid.onlinego.resources.mygames_refresh_failed
import io.zenandroid.onlinego.data.ogs.ConnectionStatus
import io.zenandroid.onlinego.data.repositories.GameSyncState
import io.zenandroid.onlinego.ui.screens.mygames.HomeStatusMessage
import io.zenandroid.onlinego.ui.screens.mygames.homeStatusMessages
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The width the avatar URL is requested at, and the box it is drawn into. They differ; the
 * startup preload in `OnlineGoApplication` mirrors both so its cache entry matches this one.
 */
val HEADER_AVATAR_URL_WIDTH = 56.dp
val HEADER_AVATAR_SIZE = 64.dp

@Composable
fun HomeScreenHeader(
  image: String? = null,
  mainText: String,
  subText: String? = null,
  connectionStatus: ConnectionStatus = ConnectionStatus.Connected,
  gameSyncState: GameSyncState = GameSyncState(),
) {
  Row(modifier = Modifier
    .statusBarsPadding()
    .padding(20.dp)
    .fillMaxWidth()) {
    AsyncImage(
      model = ImageRequest.Builder(LocalPlatformContext.current)
        .data(
          processGravatarURL(
            image,
            LocalDensity.current.run { HEADER_AVATAR_URL_WIDTH.roundToPx() })
        )
        .build(),
      placeholder = painterResource(Res.drawable.ic_person_filled_with_background),
      error = painterResource(Res.drawable.ic_person_filled_with_background),
      contentDescription = stringResource(Res.string.mygames_header_icon_content_description),
      modifier = Modifier
        .size(HEADER_AVATAR_SIZE)
        .clip(RoundedCornerShape(4.dp))
    )

    Column(modifier = Modifier.weight(1f).padding(start = 20.dp, end = 8.dp)) {
      Text(
        text = mainText,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      subText?.let {
        Text(
          text = it,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 6.dp),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    val messages = homeStatusMessages(connectionStatus, gameSyncState)
    AnimatedVisibility(
      visible = messages.isNotEmpty(),
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.CenterVertically)
    ) {
      Column(
        modifier = Modifier.widthIn(max = 100.dp),
        horizontalAlignment = Alignment.End,
      ) {
        messages.forEach { message ->
          val label = stringResource(when (message) {
            HomeStatusMessage.Connecting -> Res.string.mygames_connecting
            HomeStatusMessage.Reconnecting -> Res.string.mygames_reconnecting
            HomeStatusMessage.Offline -> Res.string.mygames_offline_content_description
            HomeStatusMessage.RefreshingGames -> Res.string.mygames_refreshing_games
            HomeStatusMessage.RefreshFailed -> Res.string.mygames_refresh_failed
          })
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (message == HomeStatusMessage.Offline) {
              Image(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.size(16.dp),
              )
            } else if (message != HomeStatusMessage.RefreshFailed) {
              CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
            }
            Text(
              text = label,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(start = 4.dp),
            )
          }
        }
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
      subText = "It's your turn in 4 games.",
      connectionStatus = ConnectionStatus.Connecting,
      gameSyncState = GameSyncState(pendingRefreshes = 1),
    )
  }
}

@Preview
@Composable
private fun Preview1() {
  OnlineGoTheme(darkTheme = true) {
    HomeScreenHeader(
      mainText = "Hi Alex,",
      subText = "It's your turn in 4 games.",
      connectionStatus = ConnectionStatus.Connected,
    )
  }
}