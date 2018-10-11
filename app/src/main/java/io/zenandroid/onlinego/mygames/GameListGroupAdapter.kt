package io.zenandroid.onlinego.mygames

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.FinishedGameItem
import io.zenandroid.onlinego.reusable.HeaderItem

/**
 * Created by 44108952 on 31/05/2018.
 */
class GameListGroupAdapter : GroupAdapter<ViewHolder>() {
    private val myMoveSection = object : Section(HeaderItem("YOUR TURN")) {
        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.notifyItemRangeInserted(positionStart, itemCount)
            recyclerView?.apply {
                if((layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() == 0) {
                    handler.postDelayed({ smoothScrollToPosition(0) }, 100)
                }
            }
        }
    }

    private val opponentMoveSection = Section(HeaderItem("OPPONENT'S TURN"))
    private val finishedGamesSection = Section(HeaderItem("RECENTLY FINISHED"))

    private var recyclerView: RecyclerView? = null

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
            val myTurn = when {
                game.phase == Phase.PLAY -> game.playerToMoveId == userId
                game.phase == Phase.STONE_REMOVAL -> {
                    val myRemovedStones = if(userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                    game.removedStones != myRemovedStones
                }
                else -> false
            }

            if(myTurn) {
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }
}