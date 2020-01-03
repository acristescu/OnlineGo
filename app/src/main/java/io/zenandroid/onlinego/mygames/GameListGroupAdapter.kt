package io.zenandroid.onlinego.mygames

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.reusable.*

/**
 * Created by 44108952 on 31/05/2018.
 */
class GameListGroupAdapter : GroupAdapter<ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null

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
    private val challengesSection = Section(HeaderItem("CHALLENGES"))
    private val automatchSection = Section()

    private var recyclerView: RecyclerView? = null

    init {
        myMoveSection.setHideWhenEmpty(true)
        opponentMoveSection.setHideWhenEmpty(true)
        finishedGamesSection.setHideWhenEmpty(true)
        challengesSection.setHideWhenEmpty(true)
        add(HeaderItem("NEW GAME"))
        val newGameAdapter = GroupAdapter<ViewHolder>()
        newGameAdapter.add(NewGameItem.AutoMatch)
        newGameAdapter.add(NewGameItem.Custom)
        newGameAdapter.setOnItemClickListener { item, view ->
            onItemClickListener?.onItemClick(item, view)
        }
        add(CarouselItem(newGameAdapter))
        add(automatchSection)
        add(challengesSection)
        add(myMoveSection)
        add(opponentMoveSection)
        add(finishedGamesSection)
    }

    override fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
        super.setOnItemClickListener(onItemClickListener)
    }

    fun setGames(games: List<Game>) {
        val userId = OGSServiceImpl.uiConfig?.user?.id
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

    fun setChallenges(challenges: List<ChallengeItem>) {
        challengesSection.update(challenges)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setAutomatches(list: List<AutomatchItem>) {
        automatchSection.update(list)
    }
}