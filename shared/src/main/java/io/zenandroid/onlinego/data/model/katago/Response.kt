@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.katago

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

sealed interface KataGoResponse {
  val id: String

  @Serializable
  data class ErrorResponse(
    override val id: String,
    val error: String? = null,
    val warning: String? = null,
    val field: String? = null,
  ) : KataGoResponse

  @Immutable
  @Serializable
  data class Response(
    override val id: String,
    val turnNumber: Int,
    val moveInfos: List<MoveInfo>,
    val rootInfo: RootInfo,
    val policy: List<Float>? = null,
    val ownership: List<Float>? = null,
    // Present only when overrideSettings.humanSLProfile + includePolicy are set; same
    // row-major, pass-index-last layout as `policy`/`ownership`. See selectHumanMove.
    val humanPolicy: List<Float>? = null,
  ) : KataGoResponse
}

@Immutable
@Serializable
data class MoveInfo(
  val move: String,
  val visits: Int,
  val winrate: Float,
  val scoreStdev: Float,
  val scoreLead: Float,
  val scoreSelfplay: Float,
  val prior: Float,
  val utility: Float,
  val lcb: Float,
  val utilityLcb: Float,
  val order: Int,
  val pv: List<String>,
  val pvVisits: Int? = null,
  val ownership: List<Float>? = null
)

@Serializable
data class RootInfo(
  val scoreLead: Float? = null,
  val scoreSelfplay: Float? = null,
  val scoreStdev: Float? = null,
  val utility: Float? = null,
  val visits: Int? = null,
  val winrate: Float? = null
)

@Serializable
data class ResponseAbreviatedJSON(
  val rootInfo: RootInfo,
  val ownership: List<Float>? = null
)