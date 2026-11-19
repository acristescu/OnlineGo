package io.zenandroid.onlinego.ui.screens.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.ui.composables.SoundLoader
import io.zenandroid.onlinego.ui.composables.SoundPlayer
import org.koin.compose.koinInject

@Composable
internal fun rememberStoneSound(loader: SoundLoader = koinInject()): () -> Unit =
  rememberGameSound("files/stone.wav", loader)

@Composable
internal fun rememberClockSound(loader: SoundLoader = koinInject()): () -> Unit =
  rememberGameSound("files/clock_tick.wav", loader)

@Composable
private fun rememberGameSound(resource: String, loader: SoundLoader): () -> Unit {
  var player by remember { mutableStateOf<SoundPlayer?>(null) }
  LaunchedEffect(loader, resource) { player = loader.load(Res.getUri(resource)) }
  DisposableEffect(player) {
    val current = player
    onDispose { current?.release() }
  }
  return { player?.play() }
}
