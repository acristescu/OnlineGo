package io.zenandroid.onlinego.utils.serializers

import io.zenandroid.onlinego.ui.screens.localai.AiRules
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Same rationale as [AiDifficultySerializer]: AiGameState is persisted as a whole-state JSON
 * blob, so an unknown or renamed rules value falls back to Japanese instead of failing
 * the restore and stranding the saved game.
 */
object AiRulesSerializer : KSerializer<AiRules> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("AiRules", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AiRules) = encoder.encodeString(value.name)

  override fun deserialize(decoder: Decoder): AiRules {
    val name = decoder.decodeString()
    return AiRules.entries.firstOrNull { it.name == name } ?: AiRules.JAPANESE
  }
}
