package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.hide
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.main.MainActivity.Companion.userId
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_active_game_card.*

class ActiveGameItem constructor(game: DbGame) : GameItem(game) {
    override fun bind(holder: ViewHolder, position: Int) {
        holder.board.boardSize = game.width
        val pos = RulesManager.replay(game, computeTerritory = false)
        holder.board.position = pos

        val userId = OGSServiceImpl.instance.uiConfig?.user?.id

        gameData?.let { gameData ->
            val opponent =
                    when (userId) {
                        gameData.players?.black?.id -> gameData.players?.white
                        gameData.players?.white?.id -> gameData.players?.black
                        else -> null
                    }
            val currentPlayer =
                    when (gameData.clock.current_player) {
                        gameData.players?.black?.id -> gameData.players?.black
                        gameData.players?.white?.id -> gameData.players?.white
                        else -> null
                    }
            holder.opponent_name.text = opponent?.username
            holder.opponent_rank.text = formatRank(egfToRank(opponent?.egf))
            holder.color_bar.setBackgroundColor(
                    if(gameData.clock.current_player == userId)
                        holder.color_bar.resources.getColor(R.color.color_type_wrong)
                    else
                        holder.color_bar.resources.getColor(R.color.colorPrimary)
            )
            holder.your_turn_label.showIf(currentPlayer?.id == userId)
            holder.color.text =
                    if(gameData.players?.black?.id == userId)
                        "black"
                    else
                        "white"
            val currentPlayerTime =
                    if(currentPlayer?.id == gameData.players?.black?.id)
                        gameData.clock.black_time
                    else
                        gameData.clock.white_time
            val timeLeft = computeTimeLeft(gameData.clock, currentPlayerTime, true).firstLine
            holder.time.text = timeLeft
        } ?: run {
            holder.opponent_name.text = ""
            holder.opponent_rank.text = ""
            holder.color_bar.setBackgroundColor( holder.color_bar.resources.getColor(R.color.colorOffWhite) )
            holder.your_turn_label.hide()
            holder.color.text = ""
            holder.time.text = ""
        }
    }

    override fun getLayout(): Int = R.layout.item_active_game_card
}