package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun TitleBar(
  title: String,
  titleIcon: ImageVector?,
  onTitleClicked: (() -> Unit)?,
  onBack: (() -> Unit)?,
  moreMenuItems: List<MoreMenuItem>,
  modifier: Modifier = Modifier,
) {
  Row (modifier = modifier) {
    onBack?.let {
      IconButton(onClick = { onBack() }) {
        Icon(Rounded.ArrowBack, "Back", tint = MaterialTheme.colors.onSurface)
      }
    }
    Spacer(modifier = Modifier.weight(.5f))
    Text(
      text = title,
      color = MaterialTheme.colors.onSurface,
      style = MaterialTheme.typography.h3,
      modifier = Modifier
        .align(Alignment.CenterVertically)
        .clickable(enabled = onTitleClicked != null) { onTitleClicked?.invoke() }
    )
    titleIcon?.let {
      Icon(
        titleIcon,
        "Game Info",
        tint = MaterialTheme.colors.onSurface,
        modifier = Modifier
          .size(18.dp)
          .align(Alignment.CenterVertically)
          .padding(start = 6.dp)
          .clickable(enabled = onTitleClicked != null) { onTitleClicked?.invoke() }
      )
    }
    Spacer(modifier = Modifier.weight(.5f))
    if(moreMenuItems.isNotEmpty()) {
      Box {
        var moreMenuOpen by rememberSaveable { mutableStateOf(false) }
        IconButton(onClick = { moreMenuOpen = true }) {
          Icon(Rounded.MoreVert, "More", tint = MaterialTheme.colors.onSurface)
        }
        DropdownMenu(
          expanded = moreMenuOpen,
          onDismissRequest = { moreMenuOpen = false },
        ) {
          moreMenuItems.forEach { item ->
            key(item) {
              DropdownMenuItem(onClick = {
                moreMenuOpen = false
                item.onClick()
              }) {
                Icon(
                  painter = rememberVectorPainter(item.icon),
                  contentDescription = item.text,
                  tint = MaterialTheme.colors.onSurface,
                )
                Text(
                  text = item.text,
                  color = MaterialTheme.colors.onSurface,
                  modifier = Modifier.padding(start = 8.dp)
                )
              }
            }
          }
        }
      }
    }
  }

}

data class MoreMenuItem(
  val text: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)
