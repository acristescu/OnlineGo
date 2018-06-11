package io.zenandroid.onlinego.spectate

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
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSServiceImpl

/**
 * Created by alex on 05/11/2017.
 */
class SpectateFragment : Fragment(), SpectateContract.View {

    @BindView(R.id.games_recycler) lateinit var gamesRecycler: RecyclerView

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: SpectateContract.Presenter
    private val adapter = SpectateAdapter()

    override var games: GameList? = null
        set(value) {
            field = value
            value?.let {
                adapter.setGames(it.results)
                adapter.clicks.subscribe(presenter::onGameSelected)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_spectate, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        gamesRecycler.adapter = adapter
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        presenter = SpectatePresenter(this, OGSServiceImpl.instance)
    }

    override fun navigateToGameScreen(game: OGSGame) {
        (activity as MainActivity).navigateToGameScreen(Game.fromOGSGame(game))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun setGameData(id: Long, gameData: GameData) {
        adapter.setGameData(id, gameData)
    }

    override fun doMove(id: Long, move: Move) {
        adapter.doMove(id, move)
    }

}