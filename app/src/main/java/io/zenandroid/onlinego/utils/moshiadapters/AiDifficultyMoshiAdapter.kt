package io.zenandroid.onlinego.utils.moshiadapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.zenandroid.onlinego.ui.screens.localai.AiDifficulty

/**
 * AiGameState is persisted as a whole-state JSON blob, keyed by AiDifficulty's Kotlin constant
 * name. A rename (or an old save predating a since-removed tier) would otherwise make
 * stateAdapter.fromJson throw and silently strand the restore flow. Falling back to a sane
 * default instead keeps renaming tiers safe going forward.
 */
class AiDifficultyMoshiAdapter {
  @ToJson
  fun toJson(difficulty: AiDifficulty): String = difficulty.name

  @FromJson
  fun fromJson(name: String): AiDifficulty =
    AiDifficulty.entries.firstOrNull { it.name == name } ?: AiDifficulty.DAN_5
}
