package io.zenandroid.onlinego.ai

import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.OverrideSettings

interface AnalysisEngine {
  val searchThreads: Int

  fun start()

  fun stop()

  suspend fun analyzeMoveSequence(
    sequence: List<Position>,
    komi: Float? = null,
    rules: String = "japanese",
    maxVisits: Int? = null,
    includeOwnership: Boolean? = null,
    includeMovesOwnership: Boolean? = null,
    includePolicy: Boolean? = null,
    overrideSettings: OverrideSettings? = null,
  ): Response
}
