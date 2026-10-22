package io.zenandroid.onlinego.ui.composables

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

internal object BundleSoundLoader : SoundLoader {
  override suspend fun load(uri: String): SoundPlayer {
    val player = audioPlayer(uri)
    return object : SoundPlayer {
      override fun play() {
        player.play()
      }

      override fun release() = player.stop()
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
internal fun audioPlayer(uri: String): AVAudioPlayer =
  AVAudioPlayer(contentsOfURL = NSURL(string = uri), error = null).apply { prepareToPlay() }
