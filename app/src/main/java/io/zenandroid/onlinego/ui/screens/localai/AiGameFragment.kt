package io.zenandroid.onlinego.ui.screens.localai

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.Manifest.permission
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
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
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding3.view.clicks
import com.toomasr.sgf4j.Sgf
import com.vmadalin.easypermissions.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.databinding.FragmentAigameBinding
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.DismissNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.NewGame
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ShowNewGameDialog
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ToggleAIBlack
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ToggleAIWhite
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
import java.io.BufferedReader
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class AiGameFragment : Fragment(), MviView<AiGameState, AiGameAction> {
    private val viewModel: AiGameViewModel by viewModel()
    private val settingsRepository: SettingsRepository by inject()
    private var bottomSheet: NewGameBottomSheet? = null
    private var initialPosition: Position? = null
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
                                .map<AiGameAction> { UserAskedForOwnership },
                        binding.nameButtonLeft.clicks()
                                .map<AiGameAction> { ToggleAIBlack },
                        binding.nameButtonRight.clicks()
                                .map<AiGameAction> { ToggleAIWhite }
                )
        ).startWith(ViewReady(initialPosition.also { initialPosition = null }))

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

        getArguments()?.getString("SGF_LOCAL")?.let { loadSGF(Uri.parse(it)) }
        getArguments()?.getString("SGF_REMOTE")?.let { loadSGF(Uri.parse(it)) }

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
        binding.nameButtonLeft.setText(if(state.enginePlaysBlack) "KataGo" else "Player")
        binding.nameButtonRight.setText(if(state.enginePlaysWhite) "KataGo" else "Player")
        if(state.newGameDialogShown && bottomSheet?.isShowing != true) {
            bottomSheet = NewGameBottomSheet(requireContext(), { size, youPlayBlack, youPlayWhite, handicap ->
                internalActions.onNext(NewGame(size, youPlayBlack, youPlayWhite, handicap))
            }, {
                if (EasyPermissions.hasPermissions(requireContext(), permission.READ_EXTERNAL_STORAGE)) {
                    var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
                    chooseFile.setType("application/x-go-sgf")
                    chooseFile = Intent.createChooser(chooseFile, "Choose a file")
                    startActivityForResult(chooseFile, 1)
                    internalActions.onNext(DismissNewGameDialog)
                } else {
                    EasyPermissions.requestPermissions(
                        host = this,
                        rationale = "App needs Read Storage permission to load files",
                        requestCode = -1,
                        permission.READ_EXTERNAL_STORAGE
                    )
                }
            }, {
                if (EasyPermissions.hasPermissions(requireContext(), permission.WRITE_EXTERNAL_STORAGE)) {
                    var chooseFile = Intent(Intent.ACTION_CREATE_DOCUMENT);
                    chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
                    chooseFile.setType("application/x-go-sgf")
                    chooseFile.putExtra(Intent.EXTRA_TITLE, "go.sgf")
                    startActivityForResult(chooseFile, 2)
                    internalActions.onNext(DismissNewGameDialog)
                } else {
                    EasyPermissions.requestPermissions(
                        host = this,
                        rationale = "App needs Write Storage permission to load files",
                        requestCode = -1,
                        permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }).apply {
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
            binding.prisonersLeft.text = it.blackCaptureCount.toString()
            binding.prisonersRight.text = it.whiteCaptureCount.toString()
            binding.komiLeft.text = "-"
            binding.komiRight.text = it.komi.toString()
        }
        binding.colorIndicatorLeft.setColorFilter(Color.BLACK)
        binding.colorIndicatorRight.setColorFilter(Color.WHITE)

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

    private fun loadSGF(data: Uri) {
        val stream = when(data.getScheme()) {
            "http", "https" -> OkHttpClient.Builder()
                .cookieJar(get<UserSessionRepository>().cookieJar)
                .followRedirects(true)
                .build().let { client ->
                    val threadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                    StrictMode.setThreadPolicy(threadPolicy) // UI thread for this intent only

                    val request = Request.Builder().url(data.toString()).build()
                    val response = client.newCall(request).execute()
                    response?.body?.byteStream()
                }
            else -> requireContext().getContentResolver().openInputStream(data)
        }
        val text = stream?.bufferedReader()?.use(BufferedReader::readText)
        Log.d("AiGameFragment", "onLoad(\"${data}\") = \"${text}\"")
        val sgf = text?.let { Sgf.createFromString(it) }
        Log.d("AiGameFragment", "SGF ${sgf.toString()}")

        val size = sgf?.getProperty("SZ")?.split(":")?.let { it.plus(it) }?.take(2)
        var pos = Position(size!![0].toInt(), size!![1].toInt())
        var move = sgf?.getRootNode()?.getNextNode()
        while (move != null) {
            Log.d("AiGameFragment", "makeMove(\"${move}\")")
            val colour = when(move.getColor()) {
                "W" -> StoneType.WHITE
                "B" -> StoneType.BLACK
                else -> null
            }!!
            if (move.getMoveString().isNullOrBlank()) {
                pos = RulesManager.makeMove(pos, pos.nextToMove, Point(-1, -1))!!
                pos.nextToMove = pos.nextToMove.opponent
            } else {
                if (pos.nextToMove != colour) {
                    pos = RulesManager.makeMove(pos, pos.nextToMove, Point(-1, -1))!!
                    pos.nextToMove = pos.nextToMove.opponent
                }
                pos = RulesManager.makeMove(pos, colour, move.getCoords().let {
                    Point(it[0], it[1])
                })!!
            }
            pos.nextToMove = pos.nextToMove.opponent
            move = move.getNextNode()
        }
        Log.d("AiGameFragment", "loadPosition(\"${pos}\")")
        initialPosition = pos
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1 && resultCode == RESULT_OK)
            data?.getData()?.let {
                loadSGF(it)
            }
        if(requestCode == 2 && resultCode == RESULT_OK)
            data?.getData()?.getPath()?.let {
                Log.d("AiGameFragment", "onSave(\"${it}\")")
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}
