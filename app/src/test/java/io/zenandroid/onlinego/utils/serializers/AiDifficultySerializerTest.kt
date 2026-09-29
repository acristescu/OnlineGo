package io.zenandroid.onlinego.utils.serializers

import io.zenandroid.onlinego.ui.screens.localai.AiDifficulty
import io.zenandroid.onlinego.utils.appJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class AiDifficultySerializerTest {

  private fun decode(name: String) =
    appJson.decodeFromString(AiDifficultySerializer, "\"$name\"")

  @Test
  fun `a recognized name round-trips`() {
    AiDifficulty.entries.forEach { difficulty ->
      val encoded = appJson.encodeToString(AiDifficultySerializer, difficulty)
      assertEquals(difficulty, appJson.decodeFromString(AiDifficultySerializer, encoded))
    }
  }

  @Test
  fun `an unrecognized or renamed name falls back to DAN_5 instead of throwing`() {
    listOf("BEGINNER", "NORMAL", "HARD", "DAN_9", "").forEach { staleName ->
      assertEquals(AiDifficulty.DAN_5, decode(staleName))
    }
  }
}
