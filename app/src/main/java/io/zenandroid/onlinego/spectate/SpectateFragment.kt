package io.zenandroid.onlinego.spectate

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.OGSService

/**
 * Created by alex on 05/11/2017.
 */
class SpectateFragment : Fragment(), SpectateContract.View {
    override var games: GameList? = null
        set(value) {
            gamesRecycler.adapter = GameAdapter(value!!)
        }

    @BindView(R.id.games_recycler) lateinit var gamesRecycler: RecyclerView

    lateinit var unbinder: Unbinder
    lateinit var presenter: SpectatePresenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_spectate, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        presenter = SpectatePresenter(this, OGSService.instance)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onStart() {
        super.onStart()
        presenter.subscribe()
    }

    override fun onStop() {
        super.onStop()
        presenter.unsubscribe()
    }

    class GameAdapter(val gameList: GameList) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount(): Int {
            return if(gameList.results == null) 0 else gameList.results!!.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.game_card, parent, false)
            return ViewHolder(view)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    }
}