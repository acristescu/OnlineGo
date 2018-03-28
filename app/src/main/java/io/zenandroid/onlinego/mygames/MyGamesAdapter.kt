package io.zenandroid.onlinego.mygames

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.reusable.GamesAdapter
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.views.BoardView

/**
 * Created by alex on 03/03/2018.
 */
class MyGamesAdapter : GamesAdapter<MyGamesAdapter.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = gameList[position]
        holder.boardView.boardSize = game.width
        gameDataMap[game.id]?.let { gameData ->
            val pos = RulesManager.replay(gameData, computeTerritory = false)

            holder.boardView.position = pos
            val userId = OGSServiceImpl.instance.uiConfig?.user?.id
            val opponent =
                    when (userId) {
                        gameData.players?.black?.id -> gameData.players?.white
                        gameData.players?.white?.id -> gameData.players?.black
                        else -> null
                    }
            val currentPlayer =
                    when (gameData.clock.current_player) {
                        gameData.players?.black?.id -> gameData.players?.black
                        gameData.players?.white?.id -> gameData.players?.white
                        else -> null
                    }
            holder.opponentName.text = opponent?.username
            holder.opponentRank.text = formatRank(egfToRank(opponent?.egf))
            holder.colorBar.setBackgroundColor(
                if(gameData.clock.current_player == userId)
                    holder.colorBar.resources.getColor(R.color.color_type_wrong)
                else
                    holder.colorBar.resources.getColor(R.color.colorPrimary)
            )
            holder.yourTurnLabel.showIf(currentPlayer?.id == userId)
            holder.colorView.text =
                    if(gameData.players?.black?.id == userId)
                        "black"
                    else
                        "white"
            val currentPlayerTime =
                    if(currentPlayer?.id == gameData.players?.black?.id)
                        gameData.clock.black_time
                    else
                        gameData.clock.white_time
            val timeLeft = computeTimeLeft(gameData.clock, currentPlayerTime, true).firstLine
            holder.timeLeft.text = timeLeft
        }
        holder.itemView.setOnClickListener {
            clicksSubject.onNext(game)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_card, parent, false)
        return ViewHolder(
                view,
                view.findViewById(R.id.board),
                view.findViewById(R.id.opponent_name),
                view.findViewById(R.id.opponent_rank),
                view.findViewById(R.id.color_bar),
                view.findViewById(R.id.your_turn_label),
                view.findViewById(R.id.color),
                view.findViewById(R.id.time)
        )
    }

    class ViewHolder(
            itemView: View,
            val boardView: BoardView,
            val opponentName: TextView,
            val opponentRank: TextView,
            val colorBar: View,
            val yourTurnLabel: TextView,
            val colorView: TextView,
            val timeLeft: TextView
    ) : RecyclerView.ViewHolder(itemView)
}