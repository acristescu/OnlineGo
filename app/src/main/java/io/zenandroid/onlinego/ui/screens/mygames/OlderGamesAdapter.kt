package io.zenandroid.onlinego.ui.screens.mygames

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.items.HistoricGameItem
import io.zenandroid.onlinego.ui.items.LoadingGameItemCard

class OlderGamesAdapter: GroupAdapter<GroupieViewHolder>() {
    data class MoreDataRequest(
            val game: Game? = null
    )

    private val needsMoreDataSubject = PublishSubject.create<MoreDataRequest>()
    val needsMoreDataObservable = needsMoreDataSubject.hide().distinctUntilChanged()

    private val allGames = mutableListOf<Game>()

    private var recyclerView: RecyclerView? = null
    private val gamesSection = Section()
    private val loadingSection = Section().apply {
        add(LoadingGameItemCard())
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val visibleItemCount: Int = layoutManager.childCount
            val totalItemCount: Int = layoutManager.itemCount
            val firstVisibleItemPosition: Int = layoutManager.findFirstVisibleItemPosition()

            if (!loading && !loadedLastPage) {
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 1 && firstVisibleItemPosition >= 0) {
                    needsMoreDataSubject.onNext(MoreDataRequest(allGames.lastOrNull()))
                }
            }
        }
    }

    var loading: Boolean = false

    var loadedLastPage: Boolean = false
        set(value) {
            if(groupCount != 0) {
                if (value && !field) {
                    remove(loadingSection)
                }
                if(field && !value) {
                    add(loadingSection)
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
            if(!loadedLastPage) {
                add(loadingSection)
            }
        }
    }

    fun isEmpty() = allGames.isEmpty()
}