package io.zenandroid.onlinego.utils.serializers

import io.zenandroid.onlinego.ui.screens.localai.AiDifficulty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * AiGameState is persisted as a whole-state JSON blob, keyed by AiDifficulty's Kotlin constant
 * name. A rename (or an old save predating a since-removed tier) would otherwise make the restore
 * throw and silently strand the flow. Falling back to a sane default instead keeps renaming tiers
 * safe going forward.
 */
object AiDifficultySerializer : KSerializer<AiDifficulty> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("AiDifficulty", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AiDifficulty) = encoder.encodeString(value.name)

  override fun deserialize(decoder: Decoder): AiDifficulty {
    val name = decoder.decodeString()
    return AiDifficulty.entries.firstOrNull { it.name == name } ?: AiDifficulty.DAN_5
  }
}
