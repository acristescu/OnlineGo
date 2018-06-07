package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_finished_game_card.*

class FinishedGameItem constructor(game: DbGame) : GameItem(game) {
    override fun bind(holder: ViewHolder, position: Int) {
        holder.board.boardSize = game.width
        gameData?.let { gameData ->
            val pos = RulesManager.replay(gameData, computeTerritory = false)

            holder.board.position = pos
            val userId = OGSServiceImpl.instance.uiConfig?.user?.id
            val opponent =
                    when (userId) {
                        gameData.players?.black?.id -> gameData.players?.white
                        gameData.players?.white?.id -> gameData.players?.black
                        else -> null
                    }
            holder.color_bar.setBackgroundColor(holder.color_bar.resources.getColor(
                    if(opponent?.id == gameData.players?.black?.id) R.color.white else R.color.colorTextSecondary
            ))

            holder.opponent_name.text = opponent?.username
            holder.opponent_rank.text = formatRank(egfToRank(opponent?.egf))
            holder.outcome.text = when {
                userId == gameData.players?.black?.id ->
                    if (game.blackLost == true) "Lost by ${game.outcome}"
                    else "Won by ${game.outcome}"
                userId == gameData.players?.white?.id ->
                    if (game.whiteLost == true) "Lost by ${game.outcome}"
                    else "Won by ${game.outcome}"
                game.whiteLost == true ->
                    "Black won by ${game.outcome}"
                else ->
                    "White won by ${game.outcome}"
            }
        } ?: run {
            holder.board.position = Position(game.width)
            holder.opponent_name.text = ""
            holder.opponent_rank.text = ""
            holder.color_bar.setBackgroundColor( holder.color_bar.resources.getColor(R.color.colorOffWhite) )
            holder.outcome.text = ""
        }
    }

    override fun getLayout(): Int = R.layout.item_finished_game_card
}