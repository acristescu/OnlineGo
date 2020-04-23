package io.zenandroid.onlinego.reusable

import androidx.core.content.res.ResourcesCompat
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_active_game_card.*

class ActiveGameItem (val game: Game) : Item(game.id) {
    override fun bind(holder: GroupieViewHolder, position: Int) {
        holder.apply {
            board.boardSize = game.width
            board.position = RulesManager.replay(game, computeTerritory = false)

            val userId = OGSServiceImpl.uiConfig?.user?.id

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
            opponent_name.text = opponent?.username
            opponent_rank.text = formatRank(egfToRank(opponent?.rating))
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

            val timerDetails = game.clock?.let {
                if (currentPlayer?.id == game.blackPlayer.id)
                    computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true)
                else
                    computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true)
            }
            time.text = timerDetails?.firstLine ?: ""
        }
    }

    override fun getLayout(): Int = R.layout.item_active_game_card
}