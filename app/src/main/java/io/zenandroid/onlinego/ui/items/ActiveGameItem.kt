package io.zenandroid.onlinego.ui.items

import androidx.core.content.res.ResourcesCompat
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_active_game_card.*

class ActiveGameItem (val game: Game) : Item(game.id) {
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
            opponent_rank.text = formatRank(egfToRank(opponent?.rating))
            opponent_rank.showIf(SettingsRepository.showRanks)
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

            color_bar.setBackgroundColor(
                    if(myTurn)
                        ResourcesCompat.getColor(color_bar.resources, R.color.color_type_wrong, null)
                    else
                        ResourcesCompat.getColor(color_bar.resources, R.color.colorPrimary, null)
            )
            your_turn_label.showIf(myTurn)
            color.text =
                    if(game.blackPlayer.id == userId) "black"
                    else "white"


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
                computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true)
            else
                computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true)
        }
        return timerDetails?.firstLine ?: ""
    }

    override fun hasSameContentAs(other: com.xwray.groupie.Item<*>?): Boolean {
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
}