package io.zenandroid.onlinego.reusable

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.views.BoardView



/**
 * Created by alex on 09/11/2017.
 */
class GameAdapter(private val gameList: MutableList<Game>) : RecyclerView.Adapter<GameAdapter.ViewHolder>() {
    private var gameDataMap = mutableMapOf<Long, GameData>()
    private val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
    private val normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL)

    private val clicksSubject = PublishSubject.create<Game>()

    val clicks: Observable<Game>
        get() = clicksSubject.hide()

    override fun getItemCount(): Int {
        return gameList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = gameList[position]
        holder.boardView.boardSize = game.width
        gameDataMap[game.id]?.let { gameData ->
            val pos = RulesManager.replay(gameData)

            holder.boardView.position = pos
            holder.blackName.text = gameData.players?.black?.username
            holder.blackRank.text = formatRank(egfToRank(gameData.players?.black?.egf))
            holder.whiteName.text = gameData.players?.white?.username
            holder.whiteRank.text = formatRank(egfToRank(gameData.players?.white?.egf))

            if(pos.getStoneAt(pos.lastMove) != StoneType.BLACK) {
                holder.blackName.typeface = boldTypeface
                holder.whiteName.typeface = normalTypeface
            } else {
                holder.blackName.typeface = normalTypeface
                holder.whiteName.typeface = boldTypeface
            }
        }
        holder.itemView.setOnClickListener({
//            game.id = 10655097
//            game.id = 10655810
            clicksSubject.onNext(game)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.game_card, parent, false)
        return ViewHolder(
                view,
                view.findViewById(R.id.board),
                view.findViewById(R.id.white_name),
                view.findViewById(R.id.white_rank),
                view.findViewById(R.id.black_name),
                view.findViewById(R.id.black_rank)
        )
    }

    fun setGameData(id: Long, gameData: GameData) {
        gameDataMap.put(id, gameData)
        notifyItemChanged(gameList.indexOfFirst { it.id == id })
    }

    fun doMove(id: Long, move: Move) {
        gameDataMap[id]?.let { gameData: GameData ->
            // TODO maybe change this to something better
            val newMoves = gameData.moves.toMutableList()
            newMoves += move.move
            gameData.moves = newMoves
        }
        notifyItemChanged(gameList.indexOfFirst { it.id == id })
    }

    class ViewHolder(
            itemView: View,
            val boardView: BoardView,
            val whiteName: TextView,
            val whiteRank: TextView,
            val blackName: TextView,
            val blackRank: TextView
    ) : RecyclerView.ViewHolder(itemView)

    fun addGame(game: Game) {
        gameList.add(game)
        notifyItemInserted(gameList.size - 1)
    }

    fun clearGames() {
        gameList.clear()
        notifyDataSetChanged()
    }
}




