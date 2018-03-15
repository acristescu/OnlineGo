package io.zenandroid.onlinego.reusable

import android.support.v7.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move



/**
 * Created by alex on 09/11/2017.
 */
abstract class GamesAdapter<T : RecyclerView.ViewHolder> : RecyclerView.Adapter<T>() {
    protected val gameDataMap = mutableMapOf<Long, GameData>()
    protected val gameList = mutableListOf<Game>()

    protected val clicksSubject = PublishSubject.create<Game>()!!

    val clicks: Observable<Game>
        get() = clicksSubject.hide()

    override fun getItemCount(): Int {
        return gameList.size
    }

    fun setGameData(id: Long, gameData: GameData) {
        gameDataMap[id] = gameData
        notifyItemChanged(gameList.indexOfFirst { it.id == id })
    }

    fun doMove(id: Long, move: Move) {
        gameDataMap[id]?.let { gameData ->
            // TODO maybe change this to something better
            val newMoves = gameData.moves.toMutableList()
            newMoves += move.move
            gameData.moves = newMoves
        }
        notifyItemChanged(gameList.indexOfFirst { it.id == id })
    }

    fun addOrReplaceGame(game: Game) {
        val index = gameList.indexOfFirst { it.id == game.id }
        if(index == -1) {
            gameList.add(game)
            notifyItemInserted(gameList.size - 1)
        } else {
            gameList[index] = game
            notifyItemChanged(index)
        }
        game.json?.let {
            setGameData(game.id, it)
        }
    }

    fun clearGames() {
        gameList.clear()
        notifyDataSetChanged()
    }

    fun setClock(id: Long, clock: Clock) {
        gameDataMap[id]?.let { gameData ->
            gameData.clock = clock
            notifyItemChanged(gameList.indexOfFirst { it.id == id })
        }

        gameList.firstOrNull { it.id == id }?.let { game ->
            if(game.player_to_move == clock.current_player) {
                return@let
            }
            game.player_to_move = clock.current_player
            val oldIndex = gameList.indexOf(game)
            gameList.remove(game)
            val insertPos = (0 until gameList.size).firstOrNull { !isMyTurn(gameList[it]) } ?: 0
            gameList.add(insertPos, game)

            notifyItemMoved(oldIndex, insertPos)
        }
    }

    fun setGames(newGames: List<Game>) {
        val toRemove = mutableListOf<Game>()
        gameList.forEach { game ->
            if (newGames.find { it.id == game.id } == null) {
                toRemove.add(game)
            }
        }
        toRemove.forEach {
            val index = gameList.indexOf(it)
            gameList.removeAt(index)
            notifyItemRemoved(index)
        }

        newGames.forEach (this::addOrReplaceGame)
    }

    fun removeGame(game: Game) {
        val index = gameList.indexOfFirst { it.id == game.id }
        if (index != -1) {
            gameList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

}




