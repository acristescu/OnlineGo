package io.zenandroid.onlinego.ui.items

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl
import kotlinx.android.synthetic.main.item_finished_game_card.*
import kotlinx.android.synthetic.main.item_finished_game_card.board
import kotlinx.android.synthetic.main.item_finished_game_card.opponent_name

class HistoricGameItem (val game: Game) : Item(game.id) {
    override fun bind(holder: GroupieViewHolder, position: Int) {
        holder.apply {
            board.boardSize = game.width
            board.drawShadow = false

            board.position = game.position

            val userId = OGSServiceImpl.uiConfig?.user?.id

            val opponent =
                    when (userId) {
                        game.blackPlayer.id -> game.whitePlayer
                        game.whitePlayer.id -> game.blackPlayer
                        else -> null
                    }

            opponent_name.text = opponent?.username

            outcome.text = when {
                userId == game.blackPlayer.id ->
                    if (game.blackLost == true) "Lost"
                    else "Won"
                userId == game.whitePlayer.id ->
                    if (game.whiteLost == true) "Lost"
                    else "Won"
                game.whiteLost == true ->
                    "Black won"
                else ->
                    "White won"
            }
        }
    }

    override fun hasSameContentAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if(other !is HistoricGameItem) {
            return false
        }
        return game.position == other.game.position &&
                game.whitePlayer.username == other.game.whitePlayer.username &&
                game.blackPlayer.username == other.game.blackPlayer.username &&
                game.outcome == other.game.outcome
    }

    override fun getLayout(): Int = R.layout.item_historic_game
}