package io.zenandroid.onlinego.newchallenge.selectopponent

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.ogs.BotsRepository
import kotlinx.android.synthetic.main.fragment_select_bot.*
import kotlinx.android.synthetic.main.item_game_info.*

class SelectBotFragment : Fragment() {

    interface OnOpponentSelected {
        fun onOpponentSelected(opponent: Player)
    }

    private val bots = Section()
    private val botsRepository = BotsRepository

    private var groupAdapter = GroupAdapter<ViewHolder>().apply {
        add(object: Item() {
            override fun bind(viewHolder: com.xwray.groupie.kotlinandroidextensions.ViewHolder, position: Int) {
                viewHolder.title.text = "Online Bots"
                viewHolder.value.text = "Online bots are AI programs run and maintained by members of the community at their expense. Playing against them requires an active internet connection."
            }

            override fun getLayout() = R.layout.item_game_info
        })
        add(bots)
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
                (parentFragment as OnOpponentSelected).onOpponentSelected(item.opponent)
            }
        }
        bots.update(botsRepository
                .bots
                .sortedBy { it.rating }
                .map(::OpponentItem))
    }

    override fun onAttach(context: Context) {
        if(parentFragment !is OnOpponentSelected) {
            throw Exception("Parent context needs to implement OnOpponentSelected")
        }
        super.onAttach(context)
    }
}