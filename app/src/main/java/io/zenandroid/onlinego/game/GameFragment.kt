package io.zenandroid.onlinego.game

import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.fadeIn
import io.zenandroid.onlinego.extensions.fadeOut
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.views.BoardView
import io.zenandroid.onlinego.views.PlayerDetailsView
import java.util.concurrent.TimeUnit

const val GAME_ID = "GAME_ID"
const val GAME_SIZE = "GAME_SIZE"

/**
 * Created by alex on 10/11/2017.
 */
class GameFragment : Fragment(), GameContract.View {
    companion object {
        fun createFragment(game: Game): GameFragment = GameFragment().apply {
            arguments = Bundle().apply {
                putLong(GAME_ID, game.id)
                putInt(GAME_SIZE, game.width)
            }
        }
    }

    @BindView(R.id.board) lateinit var board: BoardView
    @BindView(R.id.pass_button) lateinit var passButton: AppCompatImageView
    @BindView(R.id.resign_button) lateinit var resignButton: AppCompatImageView
    @BindView(R.id.previous_button) lateinit var previousButton: AppCompatImageView
    @BindView(R.id.next_button) lateinit var nextButton: AppCompatImageView
    @BindView(R.id.confirm_button) lateinit var confirmButton: AppCompatImageView
    @BindView(R.id.discard_button) lateinit var discardButton: AppCompatImageView
    @BindView(R.id.auto_button) lateinit var autoButton: AppCompatImageView
    @BindView(R.id.chat_button) lateinit var chatButton: AppCompatImageView
    @BindView(R.id.play_controls) lateinit var playControls: ViewGroup
    @BindView(R.id.white_details) lateinit var whiteDetailsView: PlayerDetailsView
    @BindView(R.id.black_details) lateinit var blackDetailsView: PlayerDetailsView


    private lateinit var unbinder: Unbinder
    private lateinit var presenter: GameContract.Presenter
    private val subscriptions = CompositeDisposable()
    private val analytics = OnlineGoApplication.instance.analytics

    override var position: Position? = null
        set(value) {
            board.position = value
            whiteDetailsView.captured = value?.whiteCapturedCount
            blackDetailsView.captured = value?.blackCapturedCount
        }

    override var komi: Float? = null
        set(value) {
            field = value
            whiteDetailsView.komi = komi
        }

    override var activePlayer: StoneType? = null
        set(value) {
            field = value
            whiteDetailsView.nextToMove = value == StoneType.WHITE
            blackDetailsView.nextToMove = value == StoneType.BLACK
        }

    override var whitePlayer: Player? = null
        set(value) {
            whiteDetailsView.player = value
        }

    override var blackPlayer: Player? = null
        set(value) {
            blackDetailsView.player = value
        }

    override var passButtonEnabled: Boolean = true
        set(value) {
            passButton.isEnabled = value
        }

    override var boardSize: Int
        get() = board.boardSize
        set(value) {
            board.boardSize = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun showFinishedDialog() {
        Toast.makeText(context, "Game finished", Toast.LENGTH_LONG).show()
    }

    override fun showYouWinDialog() {
        Toast.makeText(context, "Congratulations, you won!", Toast.LENGTH_LONG).show()
    }

    override fun showYouLoseDialog() {
        Toast.makeText(context, "Unfortunately, you lost...", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        board.isInteractive = true

        whiteDetailsView.color = StoneType.WHITE
        blackDetailsView.color = StoneType.BLACK
        analytics.logEvent("showing_game", arguments)
        presenter = GamePresenter(
                this,
                OGSServiceImpl.instance,
                (activity as MainActivity).activeGameRepository,
                arguments!!.getLong(GAME_ID),
                arguments!!.getInt(GAME_SIZE)
        )
    }

    override fun showError(t: Throwable) {
        Toast.makeText(context, "Error while talking to the server: {${t.message}", Toast.LENGTH_LONG).show()
    }

    override var showLastMove = false
        set(value) { board.drawLastMove = value }

    override var showTerritory = false
        set(value) { board.drawTerritory = value }

    override var fadeOutRemovedStones = false
        set(value) { board.fadeOutRemovedStones = value }

    override val cellSelection: Observable<Point>
        get() = board.tapUpObservable()

    override var whiteTimer: GamePresenter.TimerDetails? = null
        set(value) {
            whiteDetailsView.timerFirstLine = value?.firstLine
            whiteDetailsView.timerSecondLine = value?.secondLine
        }

    override var blackTimer: GamePresenter.TimerDetails? = null
        set(value) {
            blackDetailsView.timerFirstLine = value?.firstLine
            blackDetailsView.timerSecondLine = value?.secondLine
        }

    override fun showCandidateMove(point: Point?, nextToMove: StoneType?) {
        if(point != null) {
            analytics.logEvent("candidate_move", null)
        }
        board.showCandidateMove(point, nextToMove)
    }

    override val cellHotTrack: Observable<Point>
        get() = board.tapMoveObservable()

    override var subTitle: String? = null
        set(value) { (activity as? AppCompatActivity)?.supportActionBar?.subtitle = value }

    override var title: String? = null
        set(value) { activity?.title = value }

    override var interactive: Boolean
        get() = board.isInteractive
        set(value) { board.isInteractive = value }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override var nextButtonVisible = false
        set(value) { nextButton.showIf(value) }

    override var previousButtonVisible = false
        set(value) { previousButton.showIf(value) }

    override var chatButtonVisible = false
        set(value) { chatButton.showIf(value) }

    override var passButtonVisible = false
        set(value) { passButton.showIf(value) }

    override var resignButtonVisible = false
        set(value) { resignButton.showIf(value) }

    override var confirmButtonVisible = false
        set(value) { confirmButton.showIf(value) }

    override var discardButtonVisible = false
        set(value) { discardButton.showIf(value) }

    override var bottomBarVisible = true
        set(value) {
            if(!field && value) {
                playControls.fadeIn().subscribe()
            } else if (field && !value) {
                playControls.fadeOut().subscribe()
            }
            field = value
        }

    override var autoButtonVisible = false
        set(value) {
            autoButton.showIf(value)
            autoButton.isEnabled = value
            field = value
        }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, null)
        presenter.subscribe()
        subscriptions.add(
                repeatingPresses(previousButton)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    analytics.logEvent("previous_clicked", null)
                    presenter.onPreviousButtonPressed()
                }
        )
        subscriptions.add(
                repeatingPresses(nextButton)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            analytics.logEvent("next_clicked", null)
                            presenter.onNextButtonPressed()
                        }
        )
    }

    private fun repeatingPresses(view: View): Observable<Any> {
        return RxView.touches(view)
                .filter { it.action == MotionEvent.ACTION_DOWN || it.action == MotionEvent.ACTION_UP || it.action == MotionEvent.ACTION_CANCEL }
                .map { it.action == MotionEvent.ACTION_DOWN }
                .switchMap { state ->
                    if (state) {
                        Observable.concat(
                                Observable.just(0L),
                                Observable.interval(300, 200, TimeUnit.MILLISECONDS).take(10),
                                Observable.interval(100, 100, TimeUnit.MILLISECONDS).take(20),
                                Observable.interval(20, 20, TimeUnit.MILLISECONDS)
                        ).takeWhile { view.isEnabled }
                    } else {
                        Observable.never()
                    }
                }
    }

    override var previousButtonEnabled: Boolean = false
        set(value) { previousButton.isEnabled = value }

    override var nextButtonEnabled: Boolean = false
        set(value) { nextButton.isEnabled = value }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
        subscriptions.clear()
    }

    @OnClick(R.id.resign_button)
    fun onResignClicked() {
        analytics.logEvent("resign_clicked", null)
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to resign?")
                    .setPositiveButton("Resign", { _, _ ->
                        analytics.logEvent("resign_confirmed", null)
                        presenter.onResignConfirmed()
                    })
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    @OnClick(R.id.pass_button)
    fun onPassClicked() {
        analytics.logEvent("resign_clicked", null)
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to pass?")
                    .setPositiveButton("Pass", { _, _ ->
                        analytics.logEvent("resign_confirmed", null)
                        presenter.onPassConfirmed()
                    })
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    @OnClick(R.id.discard_button)
    fun onDiscardClicked() {
        analytics.logEvent("discard_clicked", null)
        presenter.onDiscardButtonPressed()
    }

    @OnClick(R.id.confirm_button)
    fun onConfirmClicked() {
        analytics.logEvent("confirm_clicked", null)
        presenter.onConfirmButtonPressed()
    }

    @OnClick(R.id.auto_button)
    fun onAutoClicked() {
        analytics.logEvent("auto_clicked", null)
        presenter.onAutoButtonPressed()
    }

    override fun setLoading(loading: Boolean) {
        (activity as? MainActivity)?.loading = loading
    }
}