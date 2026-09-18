package io.zenandroid.onlinego.ui.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.main.BottomNavItem
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun SenteBottomBar(
  tabs: List<BottomNavItem>,
  selectedIndex: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .clip(MaterialTheme.shapes.extraLarge)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = .95f))
        .border(
          width = 0.1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
          shape = MaterialTheme.shapes.extraLarge
        )
    ) {
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .padding(4.dp),
      ) {
        val count = tabs.size.coerceAtLeast(1)
        val slotWidth = maxWidth / count
        val target = slotWidth * selectedIndex.coerceIn(0, count - 1)
        val animatedOffset by animateDpAsState(
          targetValue = target,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
          label = "bottomBarIndicatorOffset",
        )

        Box(
          modifier = Modifier
            .offset(x = animatedOffset)
            .width(slotWidth)
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )

        Row(modifier = Modifier.fillMaxSize()) {
          tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            BottomBarTabSlot(
              tab = tab,
              selected = selected,
              onClick = { onTabSelected(index) },
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            )
          }
        }
      }
    }
  }
}

@Composable
fun bottomBarContentPadding(): Dp =
  WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 56.dp + 32.dp

@Composable
private fun BottomBarTabSlot(
  tab: BottomNavItem,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val contentColor =
    if (selected) colors.onSurface else colors.onSurfaceVariant

  Column(
    modifier = modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onClick()
      },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      painter = rememberVectorPainter(tab.icon),
      contentDescription = tab.label,
      tint = contentColor,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = tab.label,
      color = contentColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Preview
@Composable
private fun DSBottomBarPreview() {
  OnlineGoTheme {
    Surface {
      Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
        var selectedIndex by remember { mutableIntStateOf(0) }

        Text(
          text = "blablabla\nblabla\nblablabla",
          modifier = Modifier
            .fillMaxSize()
        )
        SenteBottomBar(
          tabs = listOf(
            BottomNavItem(
              "myGames",
              stringResource(R.string.bottomnavigation_botton_play),
              ImageVector.vectorResource(R.drawable.ic_board_filled)
            ),
            BottomNavItem(
              "learn",
              stringResource(R.string.bottomnavigation_botton_learn),
              ImageVector.vectorResource(R.drawable.ic_learn)
            ),
            BottomNavItem(
              "stats",
              stringResource(R.string.bottomnavigation_botton_stats),
              ImageVector.vectorResource(R.drawable.ic_diagram),
              enabled = true,
            ),
            BottomNavItem(
              "settings",
              stringResource(R.string.bottomnavigation_botton_settings),
              ImageVector.vectorResource(R.drawable.ic_settings_filled),
            ),
          ),
          selectedIndex = selectedIndex,
          onTabSelected = { selectedIndex = it },
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }
}