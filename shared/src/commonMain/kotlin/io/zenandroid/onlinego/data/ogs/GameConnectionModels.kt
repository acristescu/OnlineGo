@file:UseSerializers(
  OGSBooleanSerializer::class,
  AnySerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.local.Score
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers

@Serializable
data class Scores(
  var white: Score? = null,
  var black: Score? = null
)

@Serializable
data class OGSClock(
  var game_id: Long,
  var current_player: Long,
  var black_player_id: Long,
  var white_player_id: Long,
  var title: String? = null,
  var last_move: Long,
  var expiration: Double? = null,
  var now: Long? = null,
//    val paused_since: Long?, // NEVER USE THIS as it is set even on unpaused games
  val pause: Pause? = null,
  var start_mode: Boolean = false,
  var black_time: Any? = null,// can be number or Time object
  var white_time: Any? = null// can be number or Time object
) {
  @Transient
  var receivedAt: Long = 0
  val whiteTimeSimple get() = (white_time as? Number)?.toLong()
  val blackTimeSimple get() = (black_time as? Number)?.toLong()
  val whiteTime
    get() = (white_time as? Map<*, *>)?.let { Time.fromMap(it) }
  val blackTime
    get() = (black_time as? Map<*, *>)?.let { Time.fromMap(it) }

}

@Serializable
data class Pause(
  val pause_control: Any? = null,
  val paused: Boolean? = null,
  val paused_since: Long? = null
)

@Serializable
data class Players(
  var white: OGSPlayer? = null,
  var black: OGSPlayer? = null
)

@Serializable
data class TimeControl(
  var system: String? = null,
  var pause_on_weekends: Boolean? = null,
  var time_control: String? = null,
  var initial_time: Int? = null,
  var max_time: Int? = null,
  var time_increment: Int? = null,
  var speed: String? = null,
  var per_move: Int? = null,
  var main_time: Int? = null,
  var periods: Int? = null,
  var period_time: Int? = null,
  var stones_per_period: Int? = null,
  var total_time: Int? = null

)

//{game_id: 10528331, move_number: 202, move: [9, 17, 8509]}
@Serializable
data class Move(
  val game_id: Long,
  val move_number: Int,
  val move: MutableList<Any?>
)

//{"removed":true,"stones":"cidadfdgdieaeceifafhfighgihfhghhhiifigihii","all_removed":"daeafaecdfhfifdghgigfhghhhihcidieifigihiii"}
@Serializable
data class RemovedStones(
  val removed: Any? = null,
  val stones: String? = null,
  val all_removed: String? = null
)

@Serializable
data class RemovedStonesAccepted(
  val player_id: Long? = null,
  val stones: String? = null,
  val players: Players? = null
)

@Serializable
data class UndoAccepted(
  val move_number: Int,
  val undo_move_count: Int
)

@Serializable
data class UndoRequested(
  val move_number: Int,
  val requested_by: Long,
  val undo_move_count: Int,
)
