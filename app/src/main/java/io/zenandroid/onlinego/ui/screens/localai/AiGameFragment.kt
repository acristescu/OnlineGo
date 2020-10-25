package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Color
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.showIf
import kotlinx.android.synthetic.main.fragment_aigame.*
import kotlinx.android.synthetic.main.fragment_aigame.board
import kotlinx.android.synthetic.main.fragment_aigame.progressBar
import kotlinx.android.synthetic.main.fragment_joseki.nextButton
import kotlinx.android.synthetic.main.fragment_joseki.passButton
import kotlinx.android.synthetic.main.fragment_joseki.previousButton
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class AiGameFragment : Fragment(R.layout.fragment_aigame), MviView<AiGameState, AiGameAction> {
    private val viewModel: AiGameViewModel by viewModel()
    private var analytics = OnlineGoApplication.instance.analytics
    private val settingsRepository: SettingsRepository by inject()
    private var bottomSheet: NewGameBottomSheet? = null

    private val internalActions = PublishSubject.create<AiGameAction>()

    override val actions: Observable<AiGameAction>
        get() =             Observable.merge(
                listOf(
                        internalActions,
                        board.tapUpObservable()
                                .map<AiGameAction>(AiGameAction::UserTappedCoordinate),
                        board.tapMoveObservable()
                                .map<AiGameAction>(AiGameAction::UserHotTrackedCoordinate),
                        RxView.clicks(previousButton)
                                .map<AiGameAction> { UserPressedPrevious },
                        RxView.clicks(nextButton)
                                .map<AiGameAction> { UserPressedNext },
                        RxView.clicks(passButton)
                                .map<AiGameAction> { UserPressedPass },
                        RxView.clicks(newGameButton)
                                .map<AiGameAction> { ShowNewGameDialog },
                        RxView.clicks(hintButton)
                                .map<AiGameAction> { UserAskedForHint }
                )
        ).startWith(ViewReady)

    override fun onPause() {
        internalActions.onNext(ViewPaused)
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)
        board.apply {
            drawCoordinates = settingsRepository.showCoordinates
        }


        view?.doOnLayout {
            iconContainerLeft.radius = iconContainerLeft.width / 2f
            iconContainerRight.radius = iconContainerRight.width / 2f
            get<UserSessionRepository>().uiConfig?.user?.icon?.let {
                Glide.with(this)
                        .load(processGravatarURL(it, iconViewRight.width))
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(iconViewRight)
            }
        }

        viewModel.bind(this)
    }

    override fun render(state: AiGameState) {
        Log.v("AiGame", "rendering state=$state")
        progressBar.showIf(!state.engineStarted)
        board.apply {
            isInteractive = state.boardIsInteractive
            boardSize = state.boardSize
            drawTerritory = state.showFinalTerritory
            fadeOutRemovedStones = state.showFinalTerritory
            drawAiEstimatedOwnership = state.showAiEstimatedTerritory
            drawHints = state.showHints
            state.position?.let {
                position = it
                showCandidateMove(state.candidateMove, it.nextToMove)
            }
        }
        passButton.isEnabled = state.passButtonEnabled
        previousButton.isEnabled = state.previousButtonEnabled
        nextButton.isEnabled = state.nextButtonEnabled

        hintButton.showIf(state.hintButtonVisible)
        if(state.newGameDialogShown && bottomSheet?.isShowing != true) {
            bottomSheet = NewGameBottomSheet(requireContext()) { size, youPlayBlack, handicap ->
                internalActions.onNext(NewGame(size, youPlayBlack, handicap))
            }.apply {
                setOnCancelListener {
                    internalActions.onNext(DismissNewGameDialog)
                }
                show()
            }
        }
        if(!state.newGameDialogShown && bottomSheet?.isShowing == true) {
            bottomSheet?.dismiss()
        }
        val winrate = state.position?.aiAnalysisResult?.rootInfo?.winrate ?: state.position?.aiQuickEstimation?.winrate
        winrate?.let {
            val winrateAsPercentage = (it * 1000).toInt() / 10f
            winrateLabel.text = "White's chance to win: $winrateAsPercentage%"
            winrateProgressBar.progress = winrateAsPercentage.toInt()
        }
        state.position?.let {
            prisonersLeft.text = if(state.enginePlaysBlack) it.blackCapturedCount.toString() else it.whiteCapturedCount.toString()
            prisonersRight.text = if(state.enginePlaysBlack) it.whiteCapturedCount.toString() else it.blackCapturedCount.toString()
            komiLeft.text = if(state.enginePlaysBlack) "" else it.komi.toString()
            komiRight.text = if(state.enginePlaysBlack) it.komi.toString() else ""
        }
        colorIndicatorLeft.setColorFilter(if(state.enginePlaysBlack) Color.BLACK else Color.WHITE)
        colorIndicatorRight.setColorFilter(if(state.enginePlaysBlack) Color.WHITE else Color.BLACK)

        state.chatText?.let {
            chatBubble.visibility = VISIBLE
            chatBubble.text = it
        } ?: run { chatBubble.visibility = GONE }

        val scoreLead = state.position?.aiAnalysisResult?.rootInfo?.scoreLead ?: state.position?.aiQuickEstimation?.scoreLead
        scoreLead?.let {
            val leader = if (scoreLead > 0) "white" else "black"
            val lead = abs(scoreLead * 10).toInt() / 10f
            scoreleadLabel.text = "Score prediction: $leader leads by $lead"
        }
    }
}