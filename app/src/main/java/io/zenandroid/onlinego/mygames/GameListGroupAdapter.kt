package io.zenandroid.onlinego.mygames

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.FinishedGameItem
import io.zenandroid.onlinego.reusable.GameItem
import io.zenandroid.onlinego.reusable.HeaderItem

/**
 * Created by 44108952 on 31/05/2018.
 */
class GameListGroupAdapter : GroupAdapter<ViewHolder>() {
    private val myMoveSection = Section(HeaderItem("YOUR TURN"))
    private val opponentMoveSection = Section(HeaderItem("OPPONENT'S TURN"))
    private val finishedGamesSection = Section(HeaderItem("RECENTLY FINISHED"))

    init {
        myMoveSection.setHideWhenEmpty(true)
        opponentMoveSection.setHideWhenEmpty(true)
        finishedGamesSection.setHideWhenEmpty(true)
        add(myMoveSection)
        add(opponentMoveSection)
        add(finishedGamesSection)
    }

    fun clearGames() {
        myMoveSection.update(emptyList())
        opponentMoveSection.update(emptyList())
    }

    fun setGameData(id: Long, gameData: GameData) {
        onGameWithId(id) {
            it.gameData = gameData
            it.notifyChanged()
        }
    }

    fun doMove(id: Long, move: Move) {
        onGameWithId(id) {
            it.gameData?.let {
                val newMoves = it.moves.toMutableList()
                newMoves += move.move
                it.moves = newMoves
            }
            it.notifyChanged()
        }
    }

    fun removeGame(game: Game) {
        onGameWithId(game.id) {
            myMoveSection.removeAll(listOf(it))
            opponentMoveSection.removeAll(listOf(it))
        }
    }

    fun addOrUpdateGame(game: Game) {
        var foundGame = false
        onGameWithId(game.id) {
            it.game = game
            it.notifyChanged()
            foundGame = true
        }
        if(!foundGame) {
            val newItem = ActiveGameItem(game)
            addGameItem(newItem)
            game.json?.let {
                newItem.gameData = it
            }
        }
    }

    private fun addGameItem(item: GameItem) {
        val userId = OGSServiceImpl.instance.uiConfig?.user?.id
        if(item.gameData?.clock?.current_player == userId) {
            myMoveSection.add(item)
        } else {
            opponentMoveSection.add(item)
        }
    }

    fun setGames(games: List<Game>) {
        val userId = OGSServiceImpl.instance.uiConfig?.user?.id
        val myTurnList = mutableListOf<GameItem>()
        val opponentTurnList = mutableListOf<GameItem>()
        for(game in games) {
            val newItem = ActiveGameItem(game)
            game.json?.let {
                newItem.gameData = game.json
            }
            if(newItem.gameData?.clock?.current_player == userId) {
                myTurnList.add(newItem)
            } else {
                opponentTurnList.add(newItem)
            }
        }
        myMoveSection.update(myTurnList)
        opponentMoveSection.update(opponentTurnList)
    }

    fun setClock(id: Long, clock: Clock) {
        onGameWithId(id) { gameItem ->
            gameItem.gameData?.let {
                val currentPlayerChanged = it.clock.current_player != clock.current_player
                it.clock = clock
                if(currentPlayerChanged) {
                    gameItem.game.player_to_move = clock.current_player
                    myMoveSection.removeAll(listOf(gameItem))
                    opponentMoveSection.removeAll(listOf(gameItem))
                    addGameItem(gameItem)
                } else {
                    gameItem.notifyChanged()
                }
            }
        }
    }

    private inline fun onGameWithId(id: Long, action: (GameItem) -> Unit) {
        for (i in 0 until itemCount) {
            (getItem(i) as? GameItem)?.let { gameItem ->
                if (gameItem.id == id) {
                    action(gameItem)
                    return
                }
            }
        }
    }

    fun setHistoricGames(games: List<Game>) {
        finishedGamesSection.update(games.map(::FinishedGameItem))
    }
}