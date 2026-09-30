package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL

/**
 * The width the avatar URL is requested at, and the box it is drawn into. They differ; the
 * startup preload in `OnlineGoApplication` mirrors both so its cache entry matches this one.
 */
internal val HEADER_AVATAR_URL_WIDTH = 56.dp
internal val HEADER_AVATAR_SIZE = 64.dp

@Composable
fun HomeScreenHeader(
  image: String? = null,
  mainText: String,
  subText: String? = null,
  offline: Boolean
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
        .placeholder(R.drawable.ic_person_filled_with_background)
        .error(R.drawable.ic_person_filled_with_background)
        .build(),
      contentDescription = stringResource(R.string.mygames_header_icon_content_description),
      modifier = Modifier
        .size(HEADER_AVATAR_SIZE)
        .clip(RoundedCornerShape(4.dp))
    )

    Column(modifier = Modifier.padding(start = 30.dp)) {
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
    Spacer(modifier = Modifier.weight(1f))
    AnimatedVisibility(
      visible = offline,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.CenterVertically)
    ) {
      Image(
        imageVector = Icons.Default.CloudOff,
        contentDescription = stringResource(R.string.mygames_offline_content_description),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.size(24.dp),
      )
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
      offline = true,
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
      offline = false,
    )
  }
}