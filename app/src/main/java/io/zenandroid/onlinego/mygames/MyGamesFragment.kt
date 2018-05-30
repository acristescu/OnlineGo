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
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.id.board
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.item_game_card.*

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(), MyGamesContract.View {
    private val groupAdapter = GroupAdapter<ViewHolder>()
    private val myMoveSection = Section()
    private val opponentMoveSection = Section()

    @BindView(R.id.games_recycler) lateinit var gamesRecycler: RecyclerView

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: MyGamesContract.Presenter

    init {
        groupAdapter.add(myMoveSection)
        groupAdapter.add(opponentMoveSection)
    }

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
                is GameItem -> presenter.onGameSelected(item.game)
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

    override fun clearGames() {
        myMoveSection.update(emptyList())
        opponentMoveSection.update(emptyList())
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun setGameData(id: Long, gameData: GameData) {
        for (i in 0 until groupAdapter.itemCount) {
            (groupAdapter.getItem(i) as? GameItem)?.let {
                if(it.id == id) {
                    it.gameData = gameData
                    return
                }
            }
        }
    }

    override fun doMove(id: Long, move: Move) {
        for (i in 0 until groupAdapter.itemCount) {
            (groupAdapter.getItem(i) as? GameItem)?.let {
                if (it.id == id) {
                    it.gameData?.let {
                        val newMoves = it.moves.toMutableList()
                        newMoves += move.move
                        it.moves = newMoves
                    }
                    it.notifyChanged()
                    return
                }
            }
        }
    }

    override fun removeGame(game: Game) {
        for (i in 0 until groupAdapter.itemCount) {
            (groupAdapter.getItem(i) as? GameItem)?.let {
                if (it.id == game.id) {
                    groupAdapter.remove(it)
                }
            }
        }
    }

    override fun addGame(game: Game) {
        val newItem = GameItem(game)
        addGameItem(newItem)
        game.json?.let {
            newItem.gameData = it
        }
    }

    private fun addGameItem(item: GameItem) {
        val userId = OGSServiceImpl.instance.uiConfig?.user?.id
        if(item.gameData?.clock?.current_player == userId) {
            myMoveSection.add(item)
        } else {
            opponentMoveSection.add(item)
        }
    }

    override fun setGames(games: List<Game>) {
        val userId = OGSServiceImpl.instance.uiConfig?.user?.id
        val myTurnList = mutableListOf<GameItem>()
        val opponentTurnList = mutableListOf<GameItem>()
        for(game in games) {
            val newItem = GameItem(game)
            game.json?.let {
                newItem.gameData = game.json
            }
            if(newItem.gameData?.clock?.current_player == userId) {
                myTurnList.add(newItem)
            } else {
                opponentTurnList.add(newItem)
            }
        }
        myMoveSection.update(myTurnList)
        opponentMoveSection.update(opponentTurnList)
    }

    override fun setClock(id: Long, clock: Clock) {
        for (i in 0 until groupAdapter.itemCount) {
            (groupAdapter.getItem(i) as? GameItem)?.let { gameItem ->
                if (gameItem.id == id) {
                    gameItem.gameData?.let {
                        val currentPlayerChanged = it.clock.current_player != clock.current_player
                        it.clock = clock
                        if(currentPlayerChanged) {
                            gameItem.game.player_to_move = clock.current_player
                            groupAdapter.remove(gameItem)
                            addGameItem(gameItem)
                        } else {
                            gameItem.notifyChanged()
                        }

                    }
                }
            }
        }
    }

    override fun setLoading(loading: Boolean) {
        (activity as? MainActivity)?.loading = loading
    }

    class GameItem(val game: Game) : Item(game.id) {
        var gameData : GameData? = null

        override fun bind(holder: ViewHolder, position: Int) {
            holder.board.boardSize = game.width
            gameData?.let { gameData ->
                val pos = RulesManager.replay(gameData, computeTerritory = false)

                holder.board.position = pos
                val userId = OGSServiceImpl.instance.uiConfig?.user?.id
                val opponent =
                        when (userId) {
                            gameData.players?.black?.id -> gameData.players?.white
                            gameData.players?.white?.id -> gameData.players?.black
                            else -> null
                        }
                val currentPlayer =
                        when (gameData.clock.current_player) {
                            gameData.players?.black?.id -> gameData.players?.black
                            gameData.players?.white?.id -> gameData.players?.white
                            else -> null
                        }
                holder.opponent_name.text = opponent?.username
                holder.opponent_rank.text = formatRank(egfToRank(opponent?.egf))
                holder.color_bar.setBackgroundColor(
                        if(gameData.clock.current_player == userId)
                            holder.color_bar.resources.getColor(R.color.color_type_wrong)
                        else
                            holder.color_bar.resources.getColor(R.color.colorPrimary)
                )
                holder.your_turn_label.showIf(currentPlayer?.id == userId)
                holder.color.text =
                        if(gameData.players?.black?.id == userId)
                            "black"
                        else
                            "white"
                val currentPlayerTime =
                        if(currentPlayer?.id == gameData.players?.black?.id)
                            gameData.clock.black_time
                        else
                            gameData.clock.white_time
                val timeLeft = computeTimeLeft(gameData.clock, currentPlayerTime, true).firstLine
                holder.time_label.text = timeLeft
            }
        }

        override fun getLayout(): Int = R.layout.item_game_card

    }
}