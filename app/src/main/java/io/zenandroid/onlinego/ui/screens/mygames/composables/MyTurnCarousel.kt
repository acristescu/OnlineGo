package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.screens.mygames.Action
import kotlin.math.absoluteValue


@ExperimentalPagerApi
@ExperimentalComposeUiApi
@Composable
fun MyTurnCarousel(games: List<Game>, boardTheme: BoardTheme, userId: Long, onAction: (Action) -> Unit) {
    Column {
        val pagerState = rememberPagerState()
        HorizontalPager(
            count = games.size,
            state = pagerState,
        ) { page ->
            val game = games[page]
            Box(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                    lerp(
                        start = 0.25f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }

                    alpha = lerp(
                        start = 0.25f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
            ) {
                LargeGameItem(
                    game = game,
                    boardTheme = boardTheme,
                    userId = userId,
                    onAction = onAction,
                    modifier = Modifier.align(Center)
                )
            }
        }
        HorizontalPagerIndicator(
            pagerState = pagerState,
            activeColor = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        )
    }
}

