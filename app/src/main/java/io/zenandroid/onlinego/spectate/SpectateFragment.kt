package io.zenandroid.onlinego.spectate

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import kotlinx.android.synthetic.main.fragment_spectate.*

/**
 * Created by alex on 05/11/2017.
 */
class SpectateFragment : Fragment(), SpectateContract.View {

    private lateinit var presenter: SpectateContract.Presenter
    private val adapter = SpectateAdapter()
    private val analytics = OnlineGoApplication.instance.analytics

    override var games: GameList? = null
        set(value) {
            field = value
            value?.let {
                adapter.setGames(it.results)
                adapter.clicks.subscribe(presenter::onGameSelected)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_spectate, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        gamesRecycler.adapter = adapter
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        presenter = SpectatePresenter(this, OGSServiceImpl)
    }

    override fun navigateToGameScreen(game: OGSGame) {
        analytics.logEvent("spectate_game", Bundle().apply { putLong("GAME_ID", game.id) })
        (activity as MainActivity).navigateToGameScreen(Game.fromOGSGame(game))
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, null)
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