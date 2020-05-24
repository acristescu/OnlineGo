package io.zenandroid.onlinego.ui.items

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_finished_game_card.*
import kotlinx.android.synthetic.main.item_finished_game_card.board
import kotlinx.android.synthetic.main.item_finished_game_card.chatBadge
import kotlinx.android.synthetic.main.item_finished_game_card.chatBubble
import kotlinx.android.synthetic.main.item_finished_game_card.color_bar
import kotlinx.android.synthetic.main.item_finished_game_card.opponent_name
import kotlinx.android.synthetic.main.item_finished_game_card.opponent_rank
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.KoinContextHandler.get

class FinishedGameItem (val game: Game) : Item(game.id) {
    private val settingsRepository: SettingsRepository = get().get()

    override fun bind(holder: GroupieViewHolder, position: Int) {
        holder.apply {
            board.boardSize = game.width
            board.drawShadow = false

            board.position = game.position

            val userId = getCurrentUserId()

            val opponent =
                    when (userId) {
                        game.blackPlayer.id -> game.whitePlayer
                        game.whitePlayer.id -> game.blackPlayer
                        else -> null
                    }

            opponent_name.text = opponent?.username
            opponent_rank.text = formatRank(egfToRank(opponent?.rating))
            opponent_rank.showIf(settingsRepository.showRanks)

            chatBadge.text = game.messagesCount.toString()
            chatBadge.showIf(game.messagesCount != null && game.messagesCount != 0)
            chatBubble.showIf(game.messagesCount != null && game.messagesCount != 0)

            color_bar.setBackgroundColor(color_bar.resources.getColor(
                    if(opponent?.id == game.blackPlayer.id) R.color.whiteStones else R.color.black
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

    override fun hasSameContentAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if(other !is FinishedGameItem) {
            return false
        }
        return other.game.copy(clock = null) == game.copy(clock = null)
    }

    override fun getLayout(): Int = R.layout.item_finished_game_card
}