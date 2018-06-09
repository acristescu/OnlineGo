package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_finished_game_card.*

class FinishedGameItem (val game: DbGame) : Item(game.id) {
    override fun bind(holder: ViewHolder, position: Int) {
        holder.board.boardSize = game.width

        holder.board.position = RulesManager.replay(game, computeTerritory = false)

        val userId = OGSServiceImpl.instance.uiConfig?.user?.id

        val opponent =
                when (userId) {
                    game.blackPlayer.id -> game.whitePlayer
                    game.whitePlayer.id -> game.blackPlayer
                    else -> null
                }

        holder.opponent_name.text = opponent?.username
        holder.opponent_rank.text = formatRank(egfToRank(opponent?.rating))

        holder.color_bar.setBackgroundColor(holder.color_bar.resources.getColor(
                if(opponent?.id == game.blackPlayer.id) R.color.white else R.color.black
        ))

        holder.outcome.text = when {
            userId == game.blackPlayer.id ->
                if (game.blackLost == true) "Lost by ${game.outcome}"
                else "Won by ${game.outcome}"
            userId == game.whitePlayer.id ->
                if (game.whiteLost == true) "Lost by ${game.outcome}"
                else "Won by ${game.outcome}"
            game.whiteLost == true ->
                "Black won by ${game.outcome}"
            else ->
                "White won by ${game.outcome}"
        }
    }

    override fun getLayout(): Int = R.layout.item_finished_game_card
}