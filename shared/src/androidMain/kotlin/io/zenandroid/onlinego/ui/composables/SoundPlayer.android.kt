package io.zenandroid.onlinego.ui.composables

import android.content.res.AssetManager
import android.media.MediaDataSource
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ANDROID_ASSET_URI = "file:///android_asset/"

internal class AssetSoundLoader(private val assets: AssetManager) : SoundLoader {
  override suspend fun load(uri: String): SoundPlayer = withContext(Dispatchers.IO) {
    val bytes = assets.open(uri.removePrefix(ANDROID_ASSET_URI)).use { it.readBytes() }
    val player = MediaPlayer()
    player.setDataSource(ByteArrayMediaDataSource(bytes))
    player.prepare()
    object : SoundPlayer {
      override fun play() = player.start()
      override fun release() = player.release()
    }
  }
}

internal class ByteArrayMediaDataSource(private val bytes: ByteArray) : MediaDataSource() {
  override fun getSize(): Long = bytes.size.toLong()

  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
    if (position >= bytes.size) return -1
    val start = position.toInt()
    val count = minOf(size, bytes.size - start)
    bytes.copyInto(buffer, offset, start, start + count)
    return count
  }

  override fun close() = Unit
}
