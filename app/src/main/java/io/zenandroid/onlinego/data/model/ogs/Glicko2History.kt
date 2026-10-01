@file:UseSerializers(
  OGSBooleanSerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

data class Glicko2History(val history: List<Glicko2HistoryItem>)

//ended	game_id	played_black	handicap	rating	deviation	volatility	opponent_id	opponent_rating	opponent_deviation	outcome	extra	annulled	result
@Serializable
data class Glicko2HistoryItem(
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
        val result: String
)