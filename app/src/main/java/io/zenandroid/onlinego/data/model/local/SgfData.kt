package io.zenandroid.onlinego.data.model.local

import android.util.Log
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.utils.toEpochMicros

data class SgfData(
    val name: String?,
    var position: Position?,
    var handicap: Int?,
    val rules: String?
) {
    companion object {
        fun fromString(game: String): SgfData {
            return SgfData(
                name = null,
                handicap = null,
                rules = null,
                position = null
            )
        }
    }
}
