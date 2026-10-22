package io.zenandroid.onlinego.ui.composables

import android.content.res.AssetManager
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ANDROID_ASSET_URI = "file:///android_asset/"

internal class AssetSoundLoader(private val assets: AssetManager) : SoundLoader {
  override suspend fun load(uri: String): SoundPlayer = withContext(Dispatchers.IO) {
    val player = MediaPlayer()
    assets.openFd(uri.removePrefix(ANDROID_ASSET_URI)).use { player.setDataSource(it) }
    player.prepare()
    object : SoundPlayer {
      override fun play() = player.start()
      override fun release() = player.release()
    }
  }
}
