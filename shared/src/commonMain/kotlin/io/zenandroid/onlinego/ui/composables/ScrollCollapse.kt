package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScrollState.isCollapseTriggered(threshold: Dp = 24.dp): Boolean {
  val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
  var previousValue by remember(this) { mutableIntStateOf(value) }
  return remember(this) {
    derivedStateOf {
      val scrollingDown = value > previousValue
      previousValue = value
      value > thresholdPx && scrollingDown
    }
  }.value
}

@Composable
fun LazyListState.isCollapseTriggered(threshold: Dp = 24.dp): Boolean {
  val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
  var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
  var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
  return remember(this) {
    derivedStateOf {
      val scrollingDown = firstVisibleItemIndex > previousIndex ||
          (firstVisibleItemIndex == previousIndex && firstVisibleItemScrollOffset > previousOffset)
      previousIndex = firstVisibleItemIndex
      previousOffset = firstVisibleItemScrollOffset
      val scrolledPastThreshold =
        firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > thresholdPx
      scrolledPastThreshold && scrollingDown
    }
  }.value
}
