package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimal replacement for Accompanist's (deprecated) `HorizontalPagerIndicator`, which pulled in
 * Compose Material 2. Renders a row of dots with the current page highlighted. See
 * https://google.github.io/accompanist/pager/ — the library recommends forking this implementation.
 */
@Composable
fun HorizontalPagerIndicator(
  pagerState: PagerState,
  pageCount: Int,
  modifier: Modifier = Modifier,
  activeColor: Color = MaterialTheme.colorScheme.onSurface,
  inactiveColor: Color = activeColor.copy(alpha = 0.3f),
  indicatorSize: Dp = 8.dp,
  spacing: Dp = 8.dp,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(spacing),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(pageCount) { index ->
      val color = if (index == pagerState.currentPage) activeColor else inactiveColor
      Box(
        modifier = Modifier
          .size(indicatorSize)
          .clip(CircleShape)
          .background(color)
      )
    }
  }
}
