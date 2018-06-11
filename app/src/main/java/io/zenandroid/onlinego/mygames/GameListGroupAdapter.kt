package io.zenandroid.onlinego.mygames

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.FinishedGameItem
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

    fun setGames(games: List<Game>) {
        val userId = OGSServiceImpl.instance.uiConfig?.user?.id
        val myTurnList = mutableListOf<ActiveGameItem>()
        val opponentTurnList = mutableListOf<ActiveGameItem>()
        for(game in games) {
            val newItem = ActiveGameItem(game)
            if(game.playerToMoveId == userId) {
                myTurnList.add(newItem)
            } else {
                opponentTurnList.add(newItem)
            }
        }
        myMoveSection.update(myTurnList)
        opponentMoveSection.update(opponentTurnList)
    }

    fun setHistoricGames(games: List<Game>) {
        finishedGamesSection.update(games.map(::FinishedGameItem))
    }
}