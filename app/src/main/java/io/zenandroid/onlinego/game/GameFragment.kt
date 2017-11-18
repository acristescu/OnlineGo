package io.zenandroid.onlinego.game

import android.graphics.Point
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import io.reactivex.Observable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.Player
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.views.BoardView
import io.zenandroid.onlinego.views.PlayerDetailsView


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
    @BindView(R.id.pass_button) lateinit var passButton: AppCompatImageView
    @BindView(R.id.resign_button) lateinit var resignButton: AppCompatImageView
    @BindView(R.id.previous_button) lateinit var previousButton: AppCompatImageView
    @BindView(R.id.confirm_button) lateinit var confirmButton: AppCompatImageView
    @BindView(R.id.discard_button) lateinit var discardButton: AppCompatImageView
    @BindView(R.id.active_game_controls) lateinit var activeGameControls: ViewGroup
    @BindView(R.id.white_details) lateinit var whiteDetailsView: PlayerDetailsView
    @BindView(R.id.black_details) lateinit var blackDetailsView: PlayerDetailsView


    private lateinit var unbinder: Unbinder
    private lateinit var presenter: GameContract.Presenter
//    private val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
//    private val normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL)

    lateinit var game: Game

    override var position: Position? = null
        set(value) {
            board.position = value
            whiteDetailsView.captured = value?.whiteCapturedCount
            blackDetailsView.captured = value?.blackCapturedCount
        }

//    override var highlightBlackName: Boolean = false
//        set(value) {
//            blackNameView.typeface = if(value) boldTypeface else normalTypeface
//        }
    override var activeUIVisible: Boolean = false
        set(value) {
            activeGameControls.visibility = if(value) View.VISIBLE else View.GONE
        }

    override var whitePlayer: Player? = null
        set(value) {
            whiteDetailsView.player = value
        }

    override var blackPlayer: Player? = null
        set(value) {
            blackDetailsView.player = value
        }
    //    override var highlightWhiteName: Boolean = false
//        set(value) {
//            whiteNameView.typeface = if(value) boldTypeface else normalTypeface
//        }
    override var passButtonEnabled: Boolean = true
        set(value) {
            passButton.isEnabled = value
        }

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

    override var confirmMoveUIVisible: Boolean = false
        set(value) {
            TransitionManager.beginDelayedTransition(activeGameControls)
            for(i in 0 until activeGameControls.childCount) {
                activeGameControls.getChildAt(i).visibility = if(value) View.GONE else View.VISIBLE
            }
            confirmButton.visibility = if(value) View.VISIBLE else View.GONE
            discardButton.visibility = if(value) View.VISIBLE else View.GONE
        }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        board.isInteractive = true

        presenter = GamePresenter(this, OGSService.instance, game)
    }

    override val cellSelection: Observable<Point>
        get() = board.tapUpObservable()

    override fun showCandidateMove(point: Point?, nextToMove: StoneType?) {
        board.showCandidateMove(point, nextToMove)
    }

    override val cellHotTrack: Observable<Point>
        get() = board.tapMoveObservable()

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

    @OnClick(R.id.previous_button)
    fun onPreviousClicked() {
        presenter.onPreviousButtonPressed()
    }

    @OnClick(R.id.next_button)
    fun onNextClicked() {
        presenter.onNextButtonPressed()
    }

    @OnClick(R.id.discard_button)
    fun onDiscardClicked() {
        presenter.onDiscardButtonPressed()
    }

    @OnClick(R.id.confirm_button)
    fun onConfirmClicked() {
        presenter.onConfirmButtonPressed()
    }
}