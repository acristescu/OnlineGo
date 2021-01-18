package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.databinding.ItemFinishedGameCardBinding
import io.zenandroid.onlinego.databinding.ItemHistoricGameBinding
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId

class HistoricGameItem (val game: Game) : BindableItem<ItemHistoricGameBinding>(game.id) {
    override fun bind(binding: ItemHistoricGameBinding, position: Int) {
        binding.apply {
            board.boardSize = game.width
            board.drawShadow = false
            board.animationEnabled = false

            board.position = game.position

            val userId = getCurrentUserId()

            val opponent =
                    when (userId) {
                        game.blackPlayer.id -> game.whitePlayer
                        game.whitePlayer.id -> game.blackPlayer
                        else -> null
                    }

            opponentName.text = opponent?.username

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

    override fun hasSameContentAs(other: Item<*>): Boolean {
        if(other !is HistoricGameItem) {
            return false
        }
        return game.position == other.game.position &&
                game.whitePlayer.username == other.game.whitePlayer.username &&
                game.blackPlayer.username == other.game.blackPlayer.username &&
                game.outcome == other.game.outcome
    }

    override fun getLayout(): Int = R.layout.item_historic_game
    override fun initializeViewBinding(view: View): ItemHistoricGameBinding = ItemHistoricGameBinding.bind(view)
}