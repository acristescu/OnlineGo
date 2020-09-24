package io.zenandroid.onlinego.ui.screens.localai

import android.util.Log
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.utils.showIf
import kotlinx.android.synthetic.main.fragment_aigame.board
import kotlinx.android.synthetic.main.fragment_aigame.progressBar
import kotlinx.android.synthetic.main.fragment_joseki.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AiGameFragment : Fragment(R.layout.fragment_aigame), MviView<AiGameState, AiGameAction> {
    private val viewModel: AiGameViewModel by viewModel()
    private var analytics = OnlineGoApplication.instance.analytics
    private val settingsRepository: SettingsRepository by inject()

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
                                .map<AiGameAction> { UserPressedPass }
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
        viewModel.bind(this)
    }

    override fun render(state: AiGameState) {
        Log.d("AiGameFragment", "rendering state=$state")
        progressBar.showIf(!state.leelaStarted)
        board.apply {
            isInteractive = state.boardIsInteractive
            state.position?.let {
                position = it
                showCandidateMove(state.candidateMove, it.nextToMove)
            }
        }
        passButton.isEnabled = state.passButtonEnabled
        description.text = state.engineLog
        previousButton.isEnabled = state.previousButtonEnabled
        nextButton.isEnabled = state.nextButtonEnabled
    }
}