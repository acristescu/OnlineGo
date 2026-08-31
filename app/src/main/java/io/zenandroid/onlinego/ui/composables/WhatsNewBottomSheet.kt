package io.zenandroid.onlinego.ui.composables

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.TRANSLATING_GUIDE_URL

/**
 * Release notes. Shown automatically once after an update (see
 * [io.zenandroid.onlinego.utils.WhatsNewUtils]) and on demand from Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
  onDismiss: () -> Unit,
  onSupportClicked: () -> Unit,
) {
  val activity = LocalActivity.current
  ModalBottomSheet(
    sheetState = rememberModalBottomSheetState(true),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    WhatsNewContent(
      items = stringArrayResource(R.array.whats_new_changelog).toList(),
      onDismiss = onDismiss,
      onSupportClicked = onSupportClicked,
      onHelpTranslateClicked = {
        activity?.startActivity(Intent(Intent.ACTION_VIEW, TRANSLATING_GUIDE_URL.toUri()))
      },
    )
  }
}

@Composable
private fun WhatsNewContent(
  items: List<String>,
  onDismiss: () -> Unit,
  onSupportClicked: () -> Unit,
  onHelpTranslateClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .navigationBarsPadding()
      .padding(horizontal = 24.dp)
      .padding(bottom = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = stringResource(R.string.whats_new_title),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    // Capped so a long changelog scrolls instead of pushing the buttons off-screen.
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .heightIn(max = 300.dp)
        .verticalScroll(rememberScrollState())
        .padding(top = 20.dp, bottom = 4.dp)
    ) {
      items.forEach { item ->
        ChangelogItem(item)
      }
    }

    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      modifier = Modifier.padding(top = 16.dp)
    )

    // Gives both asks below their reason for existing.
    Text(
      text = stringResource(R.string.whats_new_open_source),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )

    FooterLink(
      icon = Icons.Rounded.Favorite,
      text = stringResource(R.string.whats_new_support_project),
      onClick = onSupportClicked,
    )
    FooterLink(
      icon = Icons.Filled.Translate,
      text = stringResource(R.string.settings_help_translate),
      onClick = onHelpTranslateClicked,
    )

    Button(
      onClick = onDismiss,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp)
    ) {
      Text(stringResource(R.string.whats_new_got_it))
    }
  }
}

/** A quiet, full-width tappable row: icon, label, chevron. */
@Composable
private fun FooterLink(
  icon: ImageVector,
  text: String,
  onClick: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
  }
}

/**
 * A bullet and its text as separate columns, so wrapped lines hang under the text rather than
 * sliding back under the bullet.
 */
@Composable
private fun ChangelogItem(text: String) {
  Row {
    Text(
      text = "•",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun WhatsNewPreview() {
  OnlineGoTheme {
    Column(modifier = Modifier.height(600.dp)) {
      Spacer(modifier = Modifier.weight(1f))
      WhatsNewContent(
        items = listOf(
          "New Look and Feel with colors chosen from your Android wallpaper",
          "OGS Moderator warnings are now displayed",
          "Prepared app for Android 15",
          "Reimplemented Face To Face, AI and Joseki screens",
          "Faster startup",
        ),
        onDismiss = {},
        onSupportClicked = {},
        onHelpTranslateClicked = {},
      )
    }
  }
}
