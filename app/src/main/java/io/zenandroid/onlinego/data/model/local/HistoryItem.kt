package io.zenandroid.onlinego.data.model.local

import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem

data class HistoryItem (
  val ended: Long,
  val gameId: Long,
  val playedBlack: Boolean,
  val handicap: Int,
  val rating: Float,
  val deviation: Float,
  val volatility: Float,
  val opponentId: Long,
  val opponentRating: Float,
  val opponentDeviation: Float,
  val won: Boolean,
  val extra: String,
  val annulled: Boolean,
  val result: String,
  val speed: String,
  val size: Int,
)

fun Glicko2HistoryItem.toHistoryItem(speed: String, size: Int) =
  HistoryItem(
    ended = ended,
    gameId = gameId,
    playedBlack = playedBlack,
    handicap = handicap,
    rating = rating,
    deviation = deviation,
    volatility = volatility,
    opponentId = opponentId,
    opponentRating = opponentRating,
    opponentDeviation = opponentDeviation,
    won = won,
    extra = extra,
    annulled = annulled,
    result = result,
    speed = speed,
    size = size,
  )
