package io.zenandroid.onlinego.utils.serializers

import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.ResponseAbreviatedJSON
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Persists only the part of a KataGo [Response] that survives a restore: the root info and a
 * coarsened ownership map. Everything else is regenerated on the next query, so dropping it keeps
 * the saved blob small.
 */
object ResponseBriefSerializer : KSerializer<Response> {
  private val surrogate = ResponseAbreviatedJSON.serializer()

  override val descriptor: SerialDescriptor = surrogate.descriptor

  override fun serialize(encoder: Encoder, value: Response) =
    encoder.encodeSerializableValue(
      surrogate,
      ResponseAbreviatedJSON(
        value.rootInfo,
        value.ownership?.map { (it * 100).toInt() / 100f }
      )
    )

  override fun deserialize(decoder: Decoder): Response {
    val json = decoder.decodeSerializableValue(surrogate)
    return Response(
      id = "",
      turnNumber = 0,
      moveInfos = persistentListOf(),
      rootInfo = json.rootInfo,
      policy = null,
      ownership = json.ownership?.toImmutableList()
    )
  }
}
