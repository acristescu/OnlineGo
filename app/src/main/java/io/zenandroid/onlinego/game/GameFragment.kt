package io.zenandroid.onlinego.game

import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import io.reactivex.Observable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.views.BoardView



/**
 * Created by alex on 10/11/2017.
 */
class GameFragment : Fragment(), GameContract.View {
    companion object {
        fun createFragment(game: Game): GameFragment {
            val fragment = GameFragment()
            fragment.game = game
            return fragment
        }
    }

    @BindView(R.id.board) lateinit var board: BoardView
    @BindView(R.id.white_name) lateinit var whiteNameView: TextView
    @BindView(R.id.white_rank) lateinit var whiteRankView: TextView
    @BindView(R.id.black_name) lateinit var blackNameView: TextView
    @BindView(R.id.black_rank) lateinit var blackRankView: TextView
    @BindView(R.id.pass_button) lateinit var passButton: Button
    @BindView(R.id.resign_button) lateinit var resignButton: Button
    @BindView(R.id.active_game_controls) lateinit var activeGameControls: View

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: GameContract.Presenter
    private val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
    private val normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL)

    lateinit var game: Game

    override var blackName: String? = null
        set(value) {
            blackNameView.text = value
        }

    override var blackRank: String? = null
        set(value) {
            blackRankView.text = value
        }

    override var whiteName: String? = null
        set(value) {
            whiteNameView.text = value
        }

    override var whiteRank: String? = null
        set(value) {
            whiteRankView.text = value
        }

    override var position: Position? = null
        set(value) {
            board.position = value
        }

    override var highlightBlackName: Boolean = false
        set(value) {
            blackNameView.typeface = if(value) boldTypeface else normalTypeface
        }
    override var activeUIVisible: Boolean = false
        set(value) {
            activeGameControls.visibility = if(value) View.VISIBLE else View.GONE
        }

    override var highlightWhiteName: Boolean = false
        set(value) {
            whiteNameView.typeface = if(value) boldTypeface else normalTypeface
        }
    override var passButtonEnabled: Boolean = true
        set(value) {passButton.isEnabled = value}

    override var boardSize: Int
        get() = board.boardSize
        set(value) {
            board.boardSize = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        board.isInteractive = true

        presenter = GamePresenter(this, OGSService.instance, game)
    }

    override val cellSelection: Observable<Point>
        get() = board.selectionObservable()

    override fun unselectMove() {
        board.clearSelection()
    }

    override var interactive: Boolean
        get() = board.isInteractive
        set(value) {board.isInteractive = value}

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

    @OnClick(R.id.resign_button)
    fun onResignClicked() {
        AlertDialog.Builder(context)
                .setTitle("Please confirm")
                .setMessage("Are you sure you want to resign?")
                .setPositiveButton("Resign", { _, _ -> presenter.onResignConfirmed() })
                .setNegativeButton(android.R.string.cancel, null).show()
    }

    @OnClick(R.id.pass_button)
    fun onPassClicked() {
        AlertDialog.Builder(context)
                .setTitle("Please confirm")
                .setMessage("Are you sure you want to pass?")
                .setPositiveButton("Pass", { _, _ -> presenter.onPassConfirmed() })
                .setNegativeButton(android.R.string.cancel, null).show()
    }
}