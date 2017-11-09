package io.zenandroid.onlinego.reusable

import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.views.BoardView

/**
 * Created by alex on 09/11/2017.
 */
class GameAdapter(private val gameList: MutableList<Game>) : RecyclerView.Adapter<GameAdapter.ViewHolder>() {
    var gameDataMap = mutableMapOf<Long, GameData>()

    override fun getItemCount(): Int {
        return gameList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        gameDataMap[gameList[position].id]?.let { gameData: GameData ->
            val pos = Position(19)
            var turn = StoneType.BLACK
            for (move in gameData.moves!!) {
                pos.makeMove(turn, Point(move[0], move[1]))
                turn = if (turn == StoneType.BLACK) StoneType.WHITE else StoneType.BLACK;
            }
            holder.boardView.position = pos
            holder.blackName.text = gameData.players?.white?.username
            holder.blackRank.text = formatRank(gameData.players?.white?.rank)
            holder.whiteName.text = gameData.players?.black?.username
            holder.whiteRank.text = formatRank(gameData.players?.black?.rank)
        }
    }

    private fun formatRank(rank: Int?): String {
        return when(rank) {
            null -> "?"
            in 0 .. 29 -> "${30 - rank} Kyu"
            in 30 .. 100 -> "${(rank - 29)} Dan"
            else -> "???"
        }
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
            val newMoves = gameData.moves?.toMutableList() ?: mutableListOf()
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
}




