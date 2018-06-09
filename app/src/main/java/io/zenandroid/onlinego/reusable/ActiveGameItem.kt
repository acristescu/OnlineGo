package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_active_game_card.*

class ActiveGameItem (val game: DbGame) : Item(game.id) {
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

        val currentPlayer =
                when (game.playerToMoveId) {
                    game.blackPlayer.id -> game.blackPlayer
                    game.whitePlayer.id -> game.whitePlayer
                    else -> null
                }
        holder.opponent_name.text = opponent?.username
        holder.opponent_rank.text = formatRank(egfToRank(opponent?.rating))
        holder.color_bar.setBackgroundColor(
                if(game.playerToMoveId == userId)
                    holder.color_bar.resources.getColor(R.color.color_type_wrong)
                else
                    holder.color_bar.resources.getColor(R.color.colorPrimary)
        )
        holder.your_turn_label.showIf(currentPlayer?.id == userId)
        holder.color.text =
                if(game.blackPlayer.id == userId) "black"
                else "white"

        val timerDetails = game.clock?.let {
            if (currentPlayer?.id == game.blackPlayer.id)
                computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true)
            else
                computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true)
        }
        holder.time.text = timerDetails?.firstLine ?: ""
    }

    override fun getLayout(): Int = R.layout.item_active_game_card
}