package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.jakewharton.rxbinding3.widget.queryTextChangeEvents
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.reactivex.Observable
import io.zenandroid.onlinego.databinding.FragmentSearchPlayerBinding
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.OpponentItem
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.SelectBotFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class SearchPlayerFragment : Fragment(), MviView<SearchPlayerState, SearchPlayerAction> {

    private val viewModel: SearchPlayerViewModel by viewModel()
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var binding: FragmentSearchPlayerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.apply{
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
        binding.searchView.isIconified = false
        binding.searchView.requestFocusFromTouch()
    }

    override fun onAttach(context: Context) {
        if(parentFragment !is SelectBotFragment.OnOpponentSelected) {
            throw Exception("Parent context needs to implement OnOpponentSelected")
        }
        super.onAttach(context)
    }

    override val actions: Observable<SearchPlayerAction>
        get() =
            binding.searchView.queryTextChangeEvents()
                    .debounce(150, TimeUnit.MILLISECONDS)
                    .distinctUntilChanged()
                    .map { SearchPlayerAction.Search(it.queryText.toString()) }

    override fun render(state: SearchPlayerState) {
        binding.progressBar.showIf(state.loading)
        groupAdapter.update(state.players.map(::OpponentItem))
    }
}