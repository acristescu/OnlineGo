package io.zenandroid.onlinego.ui.composables

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min

// Adapted from androidx.compose.material3.ExposedDropdownMenuBoxScope.ExposedDropdownMenu
//  -- Modified for purpose by using a LazyColumn instead of Column
//  -- Rationale: ExposedDropdownMenu has absolutely horrific performance otherwise
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdownMenuBoxScope.ExposedLazyDropdownMenu(
  expanded: Boolean,
  items: List<T>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset.Zero,
  scrollState: LazyListState = rememberLazyListState(),
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable (LazyItemScope.(Int, T) -> Unit)
) {
  val expandedState = remember { MutableTransitionState(false) }
  expandedState.targetState = expanded

  if (expandedState.currentState || expandedState.targetState) {
    val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
    val density = LocalDensity.current
    val popupPositionProvider = remember(density) {
      ScrollableDropdownMenuPositionProvider(
        offset,
        density,
      ) { anchorBounds, menuBounds ->
        transformOriginState.value = calculateTransformOrigin(anchorBounds, menuBounds)
      }
    }

    Popup(
      onDismissRequest = onDismissRequest,
      popupPositionProvider = popupPositionProvider,
      properties = PopupProperties(focusable = true),
    ) {
      ScrollableDropdownMenuContent(
        expandedStates = expandedState,
        transformOriginState = transformOriginState,
        state = scrollState,
        modifier = modifier.exposedDropdownSize(),
        items = items,
        verticalArrangement = verticalArrangement,
        content = content
      )
    }
  }
}

// modeled after DropdownMenuContent but uses LazyColumn instead of a scrollable column
@Composable
private fun <T> ScrollableDropdownMenuContent(
  expandedStates: MutableTransitionState<Boolean>,
  transformOriginState: MutableState<TransformOrigin>,
  state: LazyListState,
  items: List<T>,
  verticalArrangement: Arrangement.Vertical,
  modifier: Modifier = Modifier,
  content: @Composable (LazyItemScope.(Int, T) -> Unit)
) {
  // Menu open/close animation.
  val transition = updateTransition(expandedStates, "DropDownMenu")
  val inTransitionDuration = 120
  val outTransitionDuration = 75
  val scale by transition.animateFloat(
    transitionSpec = {
      if (false isTransitioningTo true) {
        // Dismissed to expanded
        tween(
          durationMillis = inTransitionDuration,
          easing = LinearOutSlowInEasing
        )
      } else {
        // Expanded to dismissed.
        tween(
          durationMillis = 1,
          delayMillis = outTransitionDuration - 1
        )
      }
    }, label = ""
  ) {
    if (it) {
      // Menu is expanded.
      1f
    } else {
      // Menu is dismissed.
      0.8f
    }
  }

  val alpha by transition.animateFloat(
    transitionSpec = {
      if (false isTransitioningTo true) {
        // Dismissed to expanded
        tween(durationMillis = 30)
      } else {
        // Expanded to dismissed.
        tween(durationMillis = outTransitionDuration)
      }
    }, label = ""
  ) {
    if (it) {
      // Menu is expanded.
      1f
    } else {
      // Menu is dismissed.
      0f
    }
  }
  Card(
    modifier = Modifier.graphicsLayer {
      scaleX = scale
      scaleY = scale
      this.alpha = alpha
      transformOrigin = transformOriginState.value
    },
  ) {
    LazyColumn(
      state = state,
      verticalArrangement = verticalArrangement,
      modifier = modifier.fillMaxWidth()
    ) {
      itemsIndexed(items) { index, item ->
        content(index, item)
      }
    }
  }
}

private data class ScrollableDropdownMenuPositionProvider(
  val contentOffset: DpOffset,
  val density: Density,
  val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
  private val menuVerticalMargin = 48.dp
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize
  ): IntOffset {
    // The min margin above and below the menu, relative to the screen.
    val verticalMargin = with(density) { menuVerticalMargin.roundToPx() }
    // The content offset specified using the dropdown offset parameter.
    val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
    val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

    // Compute horizontal position.
    val toRight = anchorBounds.left + contentOffsetX
    val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
    val toDisplayRight = windowSize.width - popupContentSize.width
    val toDisplayLeft = 0
    val x = if (layoutDirection == LayoutDirection.Ltr) {
      sequenceOf(
        toRight,
        toLeft,
        // If the anchor gets outside of the window on the left, we want to position
        // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
        if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
      )
    } else {
      sequenceOf(
        toLeft,
        toRight,
        // If the anchor gets outside of the window on the right, we want to position
        // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
        if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
      )
    }.firstOrNull {
      it >= 0 && it + popupContentSize.width <= windowSize.width
    } ?: toLeft

    // Compute vertical position.
    val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
    val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
    val toCenter = anchorBounds.top - popupContentSize.height / 2
    val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
    val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
      it >= verticalMargin &&
          it + popupContentSize.height <= windowSize.height - verticalMargin
    } ?: toTop

    onPositionCalculated(
      anchorBounds,
      IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
    )
    return IntOffset(x, y)
  }
}

private fun calculateTransformOrigin(
  parentBounds: IntRect,
  menuBounds: IntRect
): TransformOrigin {
  val pivotX = when {
    menuBounds.left >= parentBounds.right -> 0f
    menuBounds.right <= parentBounds.left -> 1f
    menuBounds.width == 0 -> 0f
    else -> {
      val intersectionCenter =
        (
            max(parentBounds.left, menuBounds.left) +
                min(parentBounds.right, menuBounds.right)
            ) / 2
      (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
    }
  }
  val pivotY = when {
    menuBounds.top >= parentBounds.bottom -> 0f
    menuBounds.bottom <= parentBounds.top -> 1f
    menuBounds.height == 0 -> 0f
    else -> {
      val intersectionCenter =
        (
            max(parentBounds.top, menuBounds.top) +
                min(parentBounds.bottom, menuBounds.bottom)
            ) / 2
      (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
    }
  }
  return TransformOrigin(pivotX, pivotY)
}
