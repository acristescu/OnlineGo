package io.zenandroid.onlinego.ui.items

import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.isPaused
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ItemActiveGameCardBinding
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import org.koin.core.context.GlobalContext.get

class ActiveGameItem (val game: Game) : BindableItem<ItemActiveGameCardBinding>(game.id) {
    private val settingsRepository: SettingsRepository = get().get()

    override fun bind(viewBinding: ItemActiveGameCardBinding, position: Int) {
        viewBinding.apply {
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

            val myTurn = when {
                game.phase == Phase.PLAY -> game.playerToMoveId == userId
                game.phase == Phase.STONE_REMOVAL -> {
                    val myRemovedStones = if(userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                    game.removedStones != myRemovedStones
                }
                else -> false
            }

            colorBar.setBackgroundColor(
                    if(myTurn)
                        ResourcesCompat.getColor(colorBar.resources, R.color.colorAccent, null)
                    else
                        ResourcesCompat.getColor(colorBar.resources, R.color.headerPrimary, null)
            )
            yourTurnLabel.showIf(myTurn)
            color.text =
                    if(game.blackPlayer.id == userId) "black"
                    else "white"

            pausedLabel.showIf(game.pauseControl.isPaused())

            time.text = calculateTimer(game)
        }
    }

    private fun calculateTimer(game: Game): String {
        val currentPlayer = when (game.playerToMoveId) {
            game.blackPlayer.id -> game.blackPlayer
            game.whitePlayer.id -> game.whitePlayer
            else -> null
        }
        val timerDetails = game.clock?.let {
            if (currentPlayer?.id == game.blackPlayer.id)
                computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true, game.pausedSince)
            else
                computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true, game.pausedSince)
        }
        return timerDetails?.firstLine ?: ""
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        if(other !is ActiveGameItem) {
            return false
        }
        var game1 = game
        var game2 = other.game
        if(calculateTimer(game1) == calculateTimer(game2)) {
            //
            // inconsequential clock change
            //
            game1 = game1.copy(clock = null)
            game2 = game2.copy(clock = null)
        }
        if(game1 == game2) {
            return true
        }
        return false
    }

    override fun getLayout(): Int = R.layout.item_active_game_card
    override fun initializeViewBinding(view: View): ItemActiveGameCardBinding = ItemActiveGameCardBinding.bind(view)
}