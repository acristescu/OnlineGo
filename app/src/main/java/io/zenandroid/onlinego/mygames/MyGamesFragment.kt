package io.zenandroid.onlinego.mygames

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.FinishedGameItem

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(), MyGamesContract.View {
    private val groupAdapter = GameListGroupAdapter()

    @BindView(R.id.games_recycler) lateinit var gamesRecycler: RecyclerView

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: MyGamesContract.Presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mygames, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        gamesRecycler.adapter = groupAdapter
        groupAdapter.setOnItemClickListener { item, _ ->
            when(item) {
                is ActiveGameItem -> presenter.onGameSelected(item.game)
                is FinishedGameItem -> presenter.onGameSelected(item.game)
            }
        }

        presenter = MyGamesPresenter(this, (activity as MainActivity).activeGameRepository)
    }

    override fun navigateToGameScreen(game: Game) {
        (activity as MainActivity).navigateToGameScreen(game)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun setHistoricGames(games: List<Game>) {
        groupAdapter.setHistoricGames(games)
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun setGames(games: List<Game>) {
        groupAdapter.setGames(games)
    }

    override fun setLoading(loading: Boolean) {
        (activity as? MainActivity)?.loading = loading
    }

}