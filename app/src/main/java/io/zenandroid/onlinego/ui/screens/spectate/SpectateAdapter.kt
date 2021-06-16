package io.zenandroid.onlinego.ui.screens.spectate

import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.ui.views.BoardView
import org.koin.core.context.GlobalContext.get

/**
 * Created by alex on 03/03/2018.
 */
@Deprecated("Obsolete")
class SpectateAdapter : GamesAdapter<SpectateAdapter.ViewHolder>() {
    private val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
    private val normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL)

    private val settingsRepository: SettingsRepository = get().get()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = gameList[position]
        holder.boardView.boardSize = game.width
        gameDataMap[game.id]?.let { gameData ->
            val pos = RulesManager.replay(gameData, computeTerritory = false)

            holder.boardView.position = pos
            holder.blackName.text = gameData.players?.black?.username
            holder.blackRank.text = formatRank(egfToRank(gameData.players?.black?.egf))
            holder.blackRank.showIf(settingsRepository.showRanks)
            holder.whiteName.text = gameData.players?.white?.username
            holder.whiteRank.text = formatRank(egfToRank(gameData.players?.white?.egf))
            holder.whiteRank.showIf(settingsRepository.showRanks)

            if(pos.getStoneAt(pos.lastMove) != StoneType.BLACK) {
                holder.blackName.typeface = boldTypeface
                holder.whiteName.typeface = normalTypeface
            } else {
                holder.blackName.typeface = normalTypeface
                holder.whiteName.typeface = boldTypeface
            }
        }
        holder.itemView.setOnClickListener {
            clicksSubject.onNext(game)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_large, parent, false)
        return ViewHolder(
                view,
                view.findViewById(R.id.board),
                view.findViewById(R.id.white_name),
                view.findViewById(R.id.white_rank),
                view.findViewById(R.id.black_name),
                view.findViewById(R.id.black_rank)
        )
    }

    class ViewHolder(
            itemView: View,
            val boardView: BoardView,
            val whiteName: TextView,
            val whiteRank: TextView,
            val blackName: TextView,
            val blackRank: TextView
    ) : RecyclerView.ViewHolder(itemView)
}