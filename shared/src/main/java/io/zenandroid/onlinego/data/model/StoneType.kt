package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class StoneType {
  BLACK,
  WHITE;

  val opponent: StoneType
    get() = if (this == BLACK) WHITE else BLACK
}
