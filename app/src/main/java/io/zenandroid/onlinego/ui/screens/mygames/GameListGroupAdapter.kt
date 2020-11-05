package io.zenandroid.onlinego.ui.screens.mygames

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.ui.items.*

/**
 * Created by alex on 31/05/2018.
 */
class GameListGroupAdapter(
        private val userId: Long?
) : GroupAdapter<GroupieViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null

    var historicGamesvisible: Boolean = false
    set(value) {
        if(field == value) {
            return
        }
        if(value && !field) {
            add(olderGamesSection)
        } else {
            remove(olderGamesSection)
        }
        field = value
    }

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

    val olderGamesAdapter = OlderGamesAdapter()

    private val opponentMoveSection = Section(HeaderItem("OPPONENT'S TURN"))
    private val recentGamesSection = Section(HeaderItem("RECENTLY FINISHED"))
    private val challengesSection = Section(HeaderItem("CHALLENGES"))
    private val automatchSection = Section()
    private val olderGamesSection = Section(HeaderItem("OLDER GAMES"), listOf(CarouselItem(olderGamesAdapter)))

    private var recyclerView: RecyclerView? = null

    init {
        myMoveSection.setHideWhenEmpty(true)
        opponentMoveSection.setHideWhenEmpty(true)
        recentGamesSection.setHideWhenEmpty(true)
        challengesSection.setHideWhenEmpty(true)
        olderGamesSection.setHideWhenEmpty(true)

        add(HeaderItem("NEW GAME"))
        val newGameAdapter = GroupAdapter<GroupieViewHolder>().apply {
            add(NewGameItem.AutoMatch)
            add(NewGameItem.Custom)
            add(NewGameItem.LocalAI)
            setOnItemClickListener { item, view ->
                onItemClickListener?.onItemClick(item, view)
            }
        }
        add(CarouselItem(newGameAdapter))
        add(automatchSection)
        add(challengesSection)
        add(myMoveSection)
        add(opponentMoveSection)
        add(recentGamesSection)
    }

    override fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
        super.setOnItemClickListener(onItemClickListener)
    }

    fun setGames(games: List<Game>) {
        val myTurnList = mutableListOf<ActiveGameItem>()
        val opponentTurnList = mutableListOf<ActiveGameItem>()
        for(game in games) {
            val newItem = ActiveGameItem(game)
            val myTurn = when (game.phase) {
                Phase.PLAY -> game.playerToMoveId == userId
                Phase.STONE_REMOVAL -> {
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

    fun setRecentGames(games: List<Game>) {
        recentGamesSection.update(games.map(::FinishedGameItem))
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