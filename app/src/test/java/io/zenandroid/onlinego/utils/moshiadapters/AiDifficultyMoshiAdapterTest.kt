package io.zenandroid.onlinego.utils.moshiadapters

import io.zenandroid.onlinego.ui.screens.localai.AiDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class AiDifficultyMoshiAdapterTest {

  private val adapter = AiDifficultyMoshiAdapter()

  @Test
  fun `a recognized name round-trips`() {
    AiDifficulty.entries.forEach { difficulty ->
      assertEquals(difficulty, adapter.fromJson(adapter.toJson(difficulty)))
    }
  }

  @Test
  fun `an unrecognized or renamed name falls back to DAN_5 instead of throwing`() {
    listOf("BEGINNER", "NORMAL", "HARD", "DAN_9", "").forEach { staleName ->
      assertEquals(AiDifficulty.DAN_5, adapter.fromJson(staleName))
    }
  }
}
