package io.zenandroid.onlinego.ogs

import io.zenandroid.onlinego.model.ogs.Player

/**
 * Created by alex on 08/11/2017.
 */
data class ActiveGameNotification(
        val id: Long,
        val phase: String,
        val name: String,
        val player_to_move: Long,
        val width: Int,
        val height: Int,
        val move_number: Int,
        val paused: Int,
        val private: Boolean,
        val time_per_move: Long,
        val black: Player,
        val White: Player
)