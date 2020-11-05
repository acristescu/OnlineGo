package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import kotlinx.android.synthetic.main.fragment_select_bot.*
import kotlinx.android.synthetic.main.item_game_info.*
import org.koin.android.ext.android.get

class SelectRecentFragment : Fragment() {

    private val recentOpponents = Section()
    private val playersRepository: PlayersRepository = get()
    private val compositeDisposable = CompositeDisposable()

    private var groupAdapter = GroupAdapter<GroupieViewHolder>().apply {
        add(object: Item() {
            override fun bind(viewHolder: GroupieViewHolder, position: Int) {
                viewHolder.title.text = "Recent opponents"
                viewHolder.value.text = "This is a selection of opponents (both bots and actual players) you've played against recently."
            }

            override fun getLayout() = R.layout.item_game_info
        })
        add(recentOpponents)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_bot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            recycler.adapter = groupAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        groupAdapter.setOnItemClickListener { item, _ ->
            if(item is OpponentItem) {
                (parentFragment as SelectBotFragment.OnOpponentSelected).onOpponentSelected(item.opponent)
            }
        }
        playersRepository.getRecentOpponents()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.map(::OpponentItem) }
                .subscribe ( recentOpponents::update, ::onError)
                .addToDisposable(compositeDisposable)
    }

    private fun onError(e: Throwable) {
        Toast.makeText(context, "An error occured when loading the recent opponents", LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        if(parentFragment !is SelectBotFragment.OnOpponentSelected) {
            throw Exception("Parent context needs to implement OnOpponentSelected")
        }
        super.onAttach(context)
    }
}