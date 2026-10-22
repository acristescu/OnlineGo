package io.zenandroid.onlinego.ai

import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.OverrideSettings

internal object UnavailableAnalysisEngine : AnalysisEngine {
  private const val MESSAGE = "Local AI is not available on iOS yet"

  override val searchThreads = 1

  override fun start() = throw UnsupportedOperationException(MESSAGE)

  override fun stop() {}

  override suspend fun analyzeMoveSequence(
    sequence: List<Position>,
    komi: Float?,
    rules: String,
    maxVisits: Int?,
    includeOwnership: Boolean?,
    includeMovesOwnership: Boolean?,
    includePolicy: Boolean?,
    overrideSettings: OverrideSettings?,
  ): Response = throw UnsupportedOperationException(MESSAGE)
}
