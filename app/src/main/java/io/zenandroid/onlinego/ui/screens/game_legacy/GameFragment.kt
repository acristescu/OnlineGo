package io.zenandroid.onlinego.ui.screens.game_legacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.touches
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.ui.screens.chat.ChatDialog
import io.zenandroid.onlinego.ui.screens.game_legacy.gameInfo.GameInfoDialog
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.databinding.FragmentGameBinding
import io.zenandroid.onlinego.ui.items.statuschips.Chip
import io.zenandroid.onlinego.ui.items.statuschips.ChipAdapter
import io.zenandroid.onlinego.ui.screens.stats.PLAYER_ID
import io.zenandroid.onlinego.utils.*
import org.koin.android.ext.android.get
import java.util.concurrent.TimeUnit

const val GAME_ID = "GAME_ID"
const val GAME_WIDTH = "GAME_WIDTH"
const val GAME_HEIGHT = "GAME_HEIGHT"

/**
 * Created by alex on 10/11/2017.
 */
@Deprecated("")
class GameFragment : Fragment(), GameContract.View {
    private lateinit var presenter: GameContract.Presenter
    private val subscriptions = CompositeDisposable()
    private val analytics = OnlineGoApplication.instance.analytics
    private val chatDialog: ChatDialog by lazy { ChatDialog() }
    private val gameInfoDialog: GameInfoDialog by lazy { GameInfoDialog() }

    private val chatClicks: Observable<Unit> by lazy { binding.chatButton.clicks() }
    private val chipAdapter = ChipAdapter()
    private var unreadCount = 0

    private lateinit var binding: FragmentGameBinding

    override var position: Position? = null
        set(value) {
            binding.board.position = value
        }

    override var whiteScore: Float = 0f
        set(value) {
            binding.whiteDetailsView.score = value
        }

    override var blackScore: Float = 0f
        set(value) {
            binding.blackDetailsView.score = value
        }

    override fun setWhitePlayerStatus(text: String?, color: Int) {
        binding.whiteDetailsView.setStatus(text, color)
    }

    override fun setBlackPlayerStatus(text: String?, color: Int) {
        binding.blackDetailsView.setStatus(text, color)
    }

    override fun setBlackPlayerPassed(passed: Boolean) {
        binding.blackDetailsView.passed = passed
    }

    override fun setWhitePlayerPassed(passed: Boolean) {
        binding.whiteDetailsView.passed = passed
    }

    override var whitePlayer: Player? = null
        set(value) {
            binding.whiteDetailsView.player = value
        }

    override var blackPlayer: Player? = null
        set(value) {
            binding.blackDetailsView.player = value
        }

    override var passButtonEnabled: Boolean = true
        set(value) {
            binding.passButton.isEnabled = value
        }

    override var hideOpponentMalkowichChat: Boolean = true
        set(value) {
            if(field != value) {
                chatDialog.hideOpponentMalkowichChat = value
            }
            field = value
        }

    override var boardWidth: Int
        get() = binding.board.boardWidth
        set(value) {
            binding.board.boardWidth = value
        }

    override var boardHeight: Int
        get() = binding.board.boardHeight
        set(value) {
            binding.board.boardHeight = value
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

    override fun showAbortedDialog() {
        Toast.makeText(context, "Game cancelled", Toast.LENGTH_LONG).show()
    }

    override fun showKoDialog() {
        Toast.makeText(context, "Illegal KO move", Toast.LENGTH_LONG).show()
    }

    override fun showAnalysisDisabledDialog() {
        context?.let {
//            AwesomeInfoDialog(context)
//                    .setTitle("Analysis is disabled")
//                    .setMessage("The challenger has configured this game to disable the analysis feature." +
//                            "This is often setup by players that wish to mimic real-life conditions, where the reading " +
//                            "of variations is visualised rather than played through.")
//                    .setDialogBodyBackgroundColor(R.color.dialogBackground)
//                    .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
//                    .setCancelable(true)
//                    .setColoredCircle(R.color.colorPrimary)
//                    .setPositiveButtonText("OK")
//                    .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
//                    .setPositiveButtonTextColor(R.color.white)
//                    .setPositiveButtonClick { }
//                    .show()
        }
    }

    override fun showChipDialog(chipType: String) {
        var title = ""
        var message = ""
        when (chipType) {
            "Playing" -> {
                title = "Playing phase"
                message = "The game is in the playing phase. Here the players will take " +
                        "turns placing stones and try to surround the most territory and capture " +
                        "opponents stones. This phase ends when both players pass their turns."
            }
            "Scoring" -> {
                title = "Scoring phase"
                message = "The game is in the scoring phase. Here the players agree on " +
                        "the dead stones so that the server can count the points. An automatic " +
                        "estimation is already provided (and you can always reset the status " +
                        "to that by pressing the wand button below) but you can make modifications " +
                        "by tapping on the stone group that you think has the wrong status. " +
                        "Once you are happy with the status of the board, you can press the " +
                        "accept button below. When both players have accepted, the game is " +
                        "over and the score is counted. If you cannot agree with your opponent " +
                        "you can cancel the scoring phase and play on to prove which " +
                        "group is alive and which is dead."
            }
            "Finished" -> {
                title = "Finished game"
                message = "The game is finished. If the outcome was decided by counting " +
                        "the points (e.g. not by timeout or one of the player resigning) " +
                        "you can see the score details by tapping on the game info button (not implemented yet)"
            }
            "Passed" -> {
                title = "Player has passed"
                message = "The last player to move has passed their turn. This means they think the " +
                        "game is over. If their opponent agrees and passes too, the game moves on " +
                        "to the scoring phase."
            }
            "Analysis" -> {
                title = "Analysis mode"
                message = "You are now in analysis mode. You can try variants here without influencing " +
                        "the real game. Simply tap on the board to see how a move would look like. " +
                        "You can navigate forwards and back in the variation. When you are done, use " +
                        "the cancel button to return to the game."
            }
            "Estimation" -> {
                title = "Score Estimation"
                message = "What you see on screen is a computer estimation of how the territory might " +
                        "be divided between the players and what the score might be if the game " +
                        "ended right now. It is very inaccurate and intended for a quick count for " +
                        "beginners and spectators. Quickly and accurately counting territory in ones head " +
                        "is a skill that is part of what makes a good GO player."
            }
        }
        context?.let {
//            AwesomeInfoDialog(context)
//                    .setTitle(title)
//                    .setMessage(message)
//                    .setDialogBodyBackgroundColor(R.color.dialogBackground)
//                    .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
//                    .setCancelable(true)
//                    .setColoredCircle(R.color.colorPrimary)
//                    .setPositiveButtonText("OK")
//                    .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
//                    .setPositiveButtonTextColor(R.color.white)
//                    .setPositiveButtonClick { }
//                    .show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        binding = FragmentGameBinding.inflate(inflater, container, false)
        binding.chipList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = chipAdapter
        }
        binding.backArrow.setOnClickListener { findNavController().navigateUp() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            board.isInteractive = true

            whiteDetailsView.color = StoneType.WHITE
            blackDetailsView.color = StoneType.BLACK

            resignButton.setOnClickListener { onResignClicked() }
            passButton.setOnClickListener { onPassClicked() }
            undoButton.setOnClickListener { presenter.onRequestUndo() }
            discardButton.setOnClickListener { presenter.onDiscardButtonPressed() }
            analyzeButton.setOnClickListener { presenter.onAnalyzeButtonClicked() }
            analysisDisabledButton.setOnClickListener { presenter.onAnalysisDisabledButtonClicked() }
            confirmButton.setOnClickListener { presenter.onConfirmButtonPressed() }
            autoButton.setOnClickListener { presenter.onAutoButtonPressed() }
            menuButton.setOnClickListener { presenter.onMenuButtonPressed() }
            whiteDetailsView.onUserClickedListener = ::onWhitePlayerPressed
            blackDetailsView.onUserClickedListener = ::onBlackPlayerPressed
        }


        analytics.logEvent("showing_game", arguments)
        presenter = GamePresenter(
                view = this,
                socketService = get(),
                userSessionRepository = get(),
                analytics = analytics,
                gameRepository = get(),
                settingsRepository = get(),
                clockDriftRepository = get(),
                gameId = requireArguments().getLong(GAME_ID),
                gameWidth = requireArguments().getInt(GAME_WIDTH),
                gameHeight = requireArguments().getInt(GAME_HEIGHT),
                chatRepository = get(),
                idlingResource = get()
        )
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun showUndoRequestConfirmation() {
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to request an undo? This will ask your opponent to allow the last move to be undone (taken back). Please note it is entirely up to them if they should accept or not.")
                    .setPositiveButton("Undo") { _, _ -> presenter.onUndoRequestConfirmed() }
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    override fun showMenu(options: List<GameContract.MenuItem>) {
        if(options.isEmpty()) {
            return
        }
         context?.let {
            MenuBottomSheet(it, options, presenter::onMenuItemSelected).show()
        }
    }

    override fun showError(t: Throwable) {
        Toast.makeText(context, "Error while talking to the server: {${t.message}", Toast.LENGTH_LONG).show()
    }

    override var showLastMove = false
        set(value) { binding.board.drawLastMove = value }

    override var showTerritory = false
        set(value) { binding.board.drawTerritory = value }

    override var showCoordinates = false
        set(value) { binding.board.drawCoordinates = value }

    override var fadeOutRemovedStones = false
        set(value) { binding.board.fadeOutRemovedStones = value }

    override val cellSelection: Observable<Cell>
        get() = binding.board.tapUpObservable()

    override var whiteTimer: GamePresenter.TimerDetails? = null
        set(value) {
            binding.whiteDetailsView.timerFirstLine = value?.firstLine
            binding.whiteDetailsView.timerSecondLine = value?.secondLine
            field = value
        }

    override var blackTimer: GamePresenter.TimerDetails? = null
        set(value) {
            binding.blackDetailsView.timerFirstLine = value?.firstLine
            binding.blackDetailsView.timerSecondLine = value?.secondLine
            field = value
        }

    override fun showCandidateMove(point: Cell?, nextToMove: StoneType?) {
        binding.board.showCandidateMove(point, nextToMove)
    }

    override val cellHotTrack: Observable<Cell>
        get() = binding.board.tapMoveObservable()

    override var title: String? = null
        set(value) {
            field = value
            binding.titleView?.text = value
        }

    override var interactive: Boolean
        get() = binding.board.isInteractive
        set(value) { binding.board.isInteractive = value }

    override fun showUndoPrompt() {
//        AwesomeInfoDialog(context)
//                .setTitle("Undo Requested")
//                .setMessage("Your opponent requested to undo his/her last move. This usually means they mis-clicked and are asking you to let them rectify the mistake. You are not obligated to do so however and can ignore their request.")
//                .setColoredCircle(R.color.colorPrimary)
//                .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
//                .setDialogBodyBackgroundColor(R.color.dialogBackground)
//                .setCancelable(true)
//                .setPositiveButtonText("Allow undo")
//                .setPositiveButtonbackgroundColor(R.color.colorPrimary)
//                .setPositiveButtonTextColor(R.color.white)
//                .setNegativeButtonText("Ignore")
//                .setNegativeButtonbackgroundColor(R.color.colorPrimary)
//                .setNegativeButtonTextColor(R.color.white)
//                .setPositiveButtonClick(presenter::onAcceptUndo)
//                .setNegativeButtonClick(presenter::onUndoRejected)
//                .show()
    }

    override var nextButtonVisible = false
        set(value) { binding.nextButton.showIf(value) }

    override var analyzeButtonVisible = false
        set(value) { binding.analyzeButton.showIf(value) }

    override var analysisDisabledButtonVisible = false
        set(value) { binding.analysisDisabledButton.showIf(value) }

    override var previousButtonVisible = false
        set(value) { binding.previousButton.showIf(value) }

    override var passButtonVisible = false
        set(value) { binding.passButton.showIf(value) }

    override var undoButtonVisible: Boolean = false
        set(value) { binding.undoButton.showIf(value) }

    override var undoButtonEnabled: Boolean = true
        set(value) { binding.undoButton.isEnabled = value }

    override var resignButtonVisible = false
        set(value) { binding.resignButton.showIf(value) }

    override var confirmButtonVisible = false
        set(value) { binding.confirmButton.showIf(value) }

    override var discardButtonVisible = false
        set(value) { binding.discardButton.showIf(value) }

    override var bottomBarVisible = true
        set(value) {
            if(!field && value) {
                binding.playControls.fadeIn().subscribe()
            } else if (field && !value) {
                binding.playControls.fadeOut().subscribe()
            }
            field = value
        }

    override var menuButtonVisible = false
        set(value) { binding.menuButton.showIf(value) }

    override var autoButtonVisible = false
        set(value) {
            binding.autoButton.showIf(value)
            binding.autoButton.isEnabled = value
            field = value
        }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.apply {
            chatClicks
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        presenter.onChatClicked()
                    }
                    .addToDisposable(subscriptions)
        }
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, javaClass.simpleName)
        repeatingPresses(binding.previousButton)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onPreviousButtonPressed() }
                .addToDisposable(subscriptions)
        repeatingPresses(binding.nextButton)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onNextButtonPressed() }
                .addToDisposable(subscriptions)
        chatDialog.sendMessage
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { presenter.onNewMessage(it) }
                .addToDisposable(subscriptions)

        presenter.subscribe()
    }

    override fun showChat() {
        fragmentManager?.findFragmentByTag("CHAT")?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commitNow()
        }
        fragmentManager?.let { chatDialog.show(it, "CHAT") }
    }

    override fun setMessageList(messages: List<Message>) {
        chatDialog.setMessages(messages)
    }

    override fun setNewMessagesCount(count: Int) {
        if(count == 0) {
            if(unreadCount != 0) {
                binding.chatBadge?.fadeOut()?.subscribe()
            }
        } else {
            if(unreadCount == 0) {
                binding.chatBadge?.fadeIn()?.subscribe()
            }
            binding.chatBadge?.text = count.toString()
        }

        unreadCount = count
    }

    override var chatMyId: Long? = null
        set(value) {
            chatDialog.setChatMyId(value)
        }

    private fun repeatingPresses(view: View): Observable<Any> {
        return view.touches()
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
        set(value) { binding.previousButton.isEnabled = value }

    override var nextButtonEnabled: Boolean = false
        set(value) { binding.nextButton.isEnabled = value }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
        subscriptions.clear()
    }

    private fun onResignClicked() {
        presenter.onResignClicked()
    }

    private fun onPassClicked() {
        presenter.onPassClicked()
    }

    private fun onWhitePlayerPressed() {
        view?.findNavController()?.navigate(
                R.id.action_gameFragment_to_statsFragment,
                bundleOf(PLAYER_ID to binding.whiteDetailsView.player!!.id)
        )
    }

    private fun onBlackPlayerPressed() {
        view?.findNavController()?.navigate(
                R.id.action_gameFragment_to_statsFragment,
                bundleOf(PLAYER_ID to binding.blackDetailsView.player!!.id)
        )
    }

    override fun showResignConfirmation() {
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to resign?")
                    .setPositiveButton("Resign") { _, _ -> presenter.onResignConfirmed() }
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    override fun showPassConfirmation() {
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to pass? This means you think the game is over and will move the game to the scoring phase if your opponent passes too.")
                    .setPositiveButton("Pass") { _, _ -> presenter.onPassConfirmed() }
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    override fun showAbortGameConfirmation() {
        context?.let {
            AlertDialog.Builder(it)
                    .setTitle("Please confirm")
                    .setMessage("Are you sure you want to abort the game? You can only do this before both players have made their first move. Your rating will not be adjusted.")
                    .setPositiveButton("Abort Game") { _, _ -> presenter.onAbortGameConfirmed() }
                    .setNegativeButton(android.R.string.cancel, null).show()
        }
    }


    override fun showGameInfoDialog(game: Game) {
        fragmentManager?.findFragmentByTag("GAME_INFO")?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
        fragmentManager?.let { gameInfoDialog.show(it, "GAME_INFO") }
        gameInfoDialog.game = game
    }

    override fun navigateTo(url: String) {
        context?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun setLoading(loading: Boolean) {
        binding.progressBar?.showIf(loading)
    }

    override fun setChips(chips: List<Chip>) {
        chipAdapter.update(chips)
    }

    override fun showInfoDialog(title: String, contents: String) {
        context?.let {
//            AwesomeInfoDialog(it)
//                    .setTitle(title)
//                    .setMessage(contents)
//                    .setCancelable(true)
//                    .setColoredCircle(R.color.colorPrimary)
//                    .setPositiveButtonText("OK")
//                    .setPositiveButtonbackgroundColor(R.color.colorPrimary)
//                    .setPositiveButtonClick { }
//                    .show()
        }
    }
}