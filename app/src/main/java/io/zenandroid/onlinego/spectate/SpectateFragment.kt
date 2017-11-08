package io.zenandroid.onlinego.spectate

import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.views.BoardView





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
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
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
        var gameDataMap = mutableMapOf<Long, GameData>()

        override fun getItemCount(): Int {
            return if(gameList.results == null) 0 else gameList.results!!.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            gameDataMap[gameList.results!![position].id]?.let { gameData: GameData ->
                val pos = Position(19)
                var turn = StoneType.BLACK
                for (move in gameData.moves!!) {
                    pos.makeMove(turn, Point(move[0], move[1]))
                    turn = if (turn == StoneType.BLACK) StoneType.WHITE else StoneType.BLACK;
                }
                holder.boardView.position = pos
                holder.blackName.text = gameData.players!!.white!!.username
                holder.blackRank.text = formatRank(gameData.players!!.white!!.rank)
                holder.whiteName.text = gameData.players!!.black!!.username
                holder.whiteRank.text = formatRank(gameData.players!!.black!!.rank)
            }
        }

        private fun formatRank(rank: Int?): String {
            return when(rank) {
                null -> "?"
                in 0 .. 29 -> "${30 - rank} Kyu"
                in 30 .. 100 -> "${(rank - 29)} Dan"
                else -> "???"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.game_card, parent, false)
            return ViewHolder(
                    view,
                    view.findViewById(R.id.board),
                    view.findViewById(R.id.white_name),
                    view.findViewById(R.id.white_rank),
                    view.findViewById(R.id.black_name),
                    view.findViewById(R.id.black_rank)
            )
        }

        fun setGameData(index: Int, gameData: GameData) {
            gameDataMap.put(gameList.results!![index].id, gameData)
            notifyItemChanged(index)
        }

        fun doMove(index: Int, move: Move) {
            gameDataMap[gameList.results!![index].id]?.let { gameData: GameData ->
                // TODO maybe change this to something better
                val newMoves = gameData.moves!!.toMutableList()
                newMoves += move.move
                gameData.moves = newMoves
            }
            notifyItemChanged(index)
        }
    }

    class ViewHolder(
            itemView: View,
            val boardView: BoardView,
            val whiteName: TextView,
            val whiteRank: TextView,
            val blackName: TextView,
            val blackRank: TextView
            ) : RecyclerView.ViewHolder(itemView)

    override fun setGameData(index: Int, gameData: GameData) {
        (gamesRecycler.adapter as GameAdapter).setGameData(index, gameData)
    }

    override fun doMove(index: Int, move: Move) {
        (gamesRecycler.adapter as GameAdapter).doMove(index, move)
    }

}