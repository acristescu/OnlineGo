package io.zenandroid.onlinego.ui.screens.mygames

import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.items.HistoricGameItem
import io.zenandroid.onlinego.ui.items.LoadingGameItemCard

class OlderGamesAdapter: GroupAdapter<GroupieViewHolder>() {

    private val needsMoreDataSubject = PublishSubject.create<MoreDataRequest>()
    val needsMoreDataObservable = needsMoreDataSubject.hide()

    private val allGames = mutableListOf<Game>()

    private var recyclerView: RecyclerView? = null
    private val gamesSection = Section()
    private val loadingSection = Section().apply {
        add(LoadingGameItemCard())
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (!recyclerView.canScrollHorizontally(1) && newState == 0) {
                needsMoreDataSubject.onNext(MoreDataRequest(allGames.lastOrNull()))
            }
        }
    }

    var loading: Boolean = true
        set(value) {
            if(groupCount != 0) {
                if (value && !field) {
                    add(loadingSection)
                }
                if(field && !value) {
                    remove(loadingSection)
                }
            }
            field = value
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(onScrollListener)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(onScrollListener)
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun appendData(games: List<Game>) {
        val newGames = games.filter { candidate -> allGames.find { candidate.id == it.id } == null }
        allGames.addAll(newGames)
        gamesSection.addAll(newGames.map(::HistoricGameItem))
        if(groupCount == 0) {
            add(gamesSection)
            if(loading) {
                add(loadingSection)
            }
        }
    }

    fun isEmpty() = allGames.isEmpty()
}

class MoreDataRequest(
        val game: Game? = null
)