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
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.PlayersRepository
import io.zenandroid.onlinego.databinding.ItemGameInfoBinding
import org.koin.android.ext.android.get

class SelectRecentFragment : Fragment() {

    private val recentOpponents = Section()
    private val playersRepository: PlayersRepository = get()
    private val compositeDisposable = CompositeDisposable()

    private var groupAdapter = GroupAdapter<GroupieViewHolder>().apply {
        add(object: BindableItem<ItemGameInfoBinding>() {
            override fun bind(binding: ItemGameInfoBinding, position: Int) {
                binding.title.text = "Recent opponents"
                binding.value.text = "This is a selection of opponents (both bots and actual players) you've played against recently."
            }

            override fun getLayout() = R.layout.item_game_info
            override fun initializeViewBinding(view: View): ItemGameInfoBinding = ItemGameInfoBinding.bind(view)
        })
        add(recentOpponents)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_bot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = groupAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
        groupAdapter.setOnItemClickListener { item, _ ->
            if(item is OpponentItem) {
                (parentFragment as SelectBotFragment.OnOpponentSelected).onOpponentSelected(item.opponent)
            }
        }
//        playersRepository.getRecentOpponents()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .map { it.map(::OpponentItem) }
//                .subscribe ( recentOpponents::update, ::onError)
//                .addToDisposable(compositeDisposable)
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