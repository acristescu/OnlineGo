package io.zenandroid.onlinego.ui.composables

interface SoundPlayer {
  fun play()
  fun release()
}

fun interface SoundLoader {
  suspend fun load(uri: String): SoundPlayer
}
