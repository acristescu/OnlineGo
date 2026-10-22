package io.zenandroid.onlinego.ui.composables

import io.zenandroid.onlinego.resources.Res
import kotlin.test.Test
import kotlin.test.assertTrue

class SoundPlayerTest {

  @Test
  fun theStoneSoundLoadsFromItsResourceUri() {
    val player = audioPlayer(Res.getUri("files/stone.wav"))
    assertTrue(player.duration in 0.17..0.19, "duration ${player.duration}")
  }
}
