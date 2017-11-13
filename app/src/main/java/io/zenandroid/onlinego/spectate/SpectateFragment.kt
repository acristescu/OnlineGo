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
import io.zenandroid.onlinego.MainActivity
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.reusable.GameAdapter





/**
 * Created by alex on 05/11/2017.
 */
class SpectateFragment : Fragment(), SpectateContract.View {

    @BindView(R.id.games_recycler) lateinit var gamesRecycler: RecyclerView

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: SpectateContract.Presenter
    private lateinit var adapter: GameAdapter

    override var games: GameList? = null
        set(value) {
            adapter = GameAdapter(value!!.results.toMutableList())
            adapter.clicks.subscribe({presenter.onGameSelected(it)})
            gamesRecycler.adapter = adapter
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_spectate, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        presenter = SpectatePresenter(this, OGSService.instance)
    }

    override fun navigateToGameScreen(game: Game) {
        (activity as MainActivity).navigateToGameScreen(game)
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

    override fun setGameData(id: Long, gameData: GameData) {
        adapter.setGameData(id, gameData)
    }

    override fun doMove(id: Long, move: Move) {
        adapter.doMove(id, move)
    }

}