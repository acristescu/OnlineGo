package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ItemFinishedGameCardBinding
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.showIf
import org.koin.core.context.KoinContextHandler.get

class FinishedGameItem (val game: Game) : BindableItem<ItemFinishedGameCardBinding>(game.id) {
    private val settingsRepository: SettingsRepository = get().get()

    override fun bind(binding: ItemFinishedGameCardBinding, position: Int) {
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
            opponentRank.text = formatRank(egfToRank(opponent?.rating))
            opponentRank.showIf(settingsRepository.showRanks)

            chatBadge.text = game.messagesCount.toString()
            chatBadge.showIf(game.messagesCount != null && game.messagesCount != 0)
            chatBubble.showIf(game.messagesCount != null && game.messagesCount != 0)

            colorBar.setBackgroundColor(colorBar.resources.getColor(
                    if(opponent?.id == game.blackPlayer.id) R.color.white else R.color.black
            ))

            outcome.text = when {
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
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        if(other !is FinishedGameItem) {
            return false
        }
        return other.game.copy(clock = null) == game.copy(clock = null)
    }

    override fun getLayout(): Int = R.layout.item_finished_game_card
    override fun initializeViewBinding(view: View): ItemFinishedGameCardBinding = ItemFinishedGameCardBinding.bind(view)
}