package io.zenandroid.onlinego.newchallenge.selectopponent.searchplayer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.jakewharton.rxbinding2.widget.RxSearchView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.reactivex.Observable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.newchallenge.selectopponent.OpponentItem
import io.zenandroid.onlinego.newchallenge.selectopponent.SelectBotFragment
import kotlinx.android.synthetic.main.fragment_search_player.*
import java.util.concurrent.TimeUnit

class SearchPlayerFragment : Fragment(R.layout.fragment_search_player), MviView<SearchPlayerState, SearchPlayerAction> {

    private lateinit var viewModel: SearchPlayerViewModel
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler.apply{
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(context)
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        groupAdapter.setOnItemClickListener { item, _ ->
            if(item is OpponentItem) {
                (parentFragment as SelectBotFragment.OnOpponentSelected).onOpponentSelected(item.opponent)
            }
        }
    }

    override fun onPause() {
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.bind(this)
        search_view.isIconified = false
        search_view.requestFocusFromTouch()
    }

    override fun onAttach(context: Context) {
        if(parentFragment !is SelectBotFragment.OnOpponentSelected) {
            throw Exception("Parent context needs to implement OnOpponentSelected")
        }
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory {
                    SearchPlayerViewModel(
                            Store(
                                    SearchPlayerReducer(),
                                    listOf(SearchMiddleware()),
                                    SearchPlayerState()
                            )
                    )
                }
        ).get(SearchPlayerViewModel::class.java)
    }

    override val actions: Observable<SearchPlayerAction>
        get() =
            RxSearchView.queryTextChangeEvents(search_view)
                    .debounce(150, TimeUnit.MILLISECONDS)
                    .distinctUntilChanged()
                    .map { SearchPlayerAction.Search(it.queryText().toString()) }

    override fun render(state: SearchPlayerState) {
        progressBar.showIf(state.loading)
        groupAdapter.update(state.players.map(::OpponentItem))
    }

    private inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
            }

}