package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.databinding.FragmentAigameBinding
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.DismissNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.NewGame
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ShowNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserAskedForHint
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserAskedForOwnership
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedNext
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedPass
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserPressedPrevious
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewPaused
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewReady
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.showIf
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class AiGameFragment : Fragment(), MviView<AiGameState, AiGameAction> {
    private val viewModel: AiGameViewModel by viewModel()
    private val settingsRepository: SettingsRepository by inject()
    private var bottomSheet: NewGameBottomSheet? = null
    private lateinit var binding: FragmentAigameBinding

    private val internalActions = PublishSubject.create<AiGameAction>()

    override val actions: Observable<AiGameAction>
        get() =             Observable.merge(
                listOf(
                        internalActions,
                        binding.board.tapUpObservable()
                                .map<AiGameAction>(AiGameAction::UserTappedCoordinate),
                        binding.board.tapMoveObservable()
                                .map<AiGameAction>(AiGameAction::UserHotTrackedCoordinate),
                        binding.previousButton.clicks()
                                .map<AiGameAction> { UserPressedPrevious },
                        binding.nextButton.clicks()
                                .map<AiGameAction> { UserPressedNext },
                        binding.passButton.clicks()
                                .map<AiGameAction> { UserPressedPass },
                        binding.newGameButton.clicks()
                                .map<AiGameAction> { ShowNewGameDialog },
                        binding.hintButton.clicks()
                                .map<AiGameAction> { UserAskedForHint },
                        binding.ownershipButton.clicks()
                                .map<AiGameAction> { UserAskedForOwnership }
                )
        ).startWith(ViewReady)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAigameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onPause() {
        internalActions.onNext(ViewPaused)
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("AiGame")
        binding.board.apply {
            drawCoordinates = settingsRepository.showCoordinates
        }


        view?.doOnLayout {
            binding.iconContainerLeft.radius = binding.iconContainerLeft.width / 2f
            binding.iconContainerRight.radius = binding.iconContainerRight.width / 2f
            get<UserSessionRepository>().uiConfig?.user?.icon?.let {
                Glide.with(this)
                        .load(processGravatarURL(it, binding.iconViewRight.width))
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(binding.iconViewRight)
            }
        }

        viewModel.bind(this)
    }

    override fun render(state: AiGameState) {
        Log.v("AiGame", "rendering state=$state")
        binding.progressBar.showIf(!state.engineStarted)
        binding.board.apply {
            isInteractive = state.boardIsInteractive
            boardWidth = state.boardSize
            boardHeight = state.boardSize
            drawTerritory = state.showFinalTerritory
            fadeOutRemovedStones = state.showFinalTerritory
            drawAiEstimatedOwnership = state.showAiEstimatedTerritory
            ownership = state.aiAnalysis?.ownership
            hints = if(state.showHints) state.aiAnalysis?.moveInfos else null
            state.position?.let {
                position = it
                showCandidateMove(state.candidateMove, it.nextToMove)
            }
        }
        binding.passButton.isEnabled = state.passButtonEnabled
        binding.previousButton.isEnabled = state.previousButtonEnabled
        binding.nextButton.isEnabled = state.nextButtonEnabled

        binding.hintButton.showIf(state.hintButtonVisible)
        binding.ownershipButton.showIf(state.ownershipButtonVisible)
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
        val winrate = state.aiAnalysis?.rootInfo?.winrate ?: state.aiQuickEstimation?.winrate
        winrate?.let {
            val winrateAsPercentage = (it * 1000).toInt() / 10f
            binding.winrateLabel.text = "White's chance to win: $winrateAsPercentage%"
            binding.winrateProgressBar.progress = winrateAsPercentage.toInt()
        }
        state.position?.let {
            binding.prisonersLeft.text = if(state.enginePlaysBlack) it.blackCaptureCount.toString() else it.whiteCaptureCount.toString()
            binding.prisonersRight.text = if(state.enginePlaysBlack) it.whiteCaptureCount.toString() else it.blackCaptureCount.toString()
            binding.komiLeft.text = if(state.enginePlaysBlack) "" else it.komi.toString()
            binding.komiRight.text = if(state.enginePlaysBlack) it.komi.toString() else ""
        }
        binding.colorIndicatorLeft.setColorFilter(if(state.enginePlaysBlack) Color.BLACK else Color.WHITE)
        binding.colorIndicatorRight.setColorFilter(if(state.enginePlaysBlack) Color.WHITE else Color.BLACK)

        state.chatText?.let {
            binding.chatBubble.visibility = VISIBLE
            binding.chatBubble.text = it
        } ?: run { binding.chatBubble.visibility = GONE }

        val scoreLead = state.aiAnalysis?.rootInfo?.scoreLead ?: state.aiQuickEstimation?.scoreLead
        scoreLead?.let {
            val leader = if (scoreLead > 0) "white" else "black"
            val lead = abs(scoreLead * 10).toInt() / 10f
            binding.scoreleadLabel.text = "Score prediction: $leader leads by $lead"
        }
    }
}