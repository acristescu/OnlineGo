package io.zenandroid.onlinego.joseki

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.jakewharton.rxbinding2.view.RxView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.settings.SettingsRepository
import io.zenandroid.onlinego.utils.PersistenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_joseki.*
import kotlinx.android.synthetic.main.fragment_joseki.progressBar

private const val TAG = "JosekiExplorerFragment"

class JosekiExplorerFragment : Fragment(R.layout.fragment_joseki), MviView<JosekiExplorerState, JosekiExplorerAction> {
    private lateinit var viewModel: JosekiExplorerViewModel
    private val markwon by lazy { buildMarkwon() }

    private val internalActions = PublishSubject.create<JosekiExplorerAction>()
    private var currentState: JosekiExplorerState? = null
    private var analytics = OnlineGoApplication.instance.analytics

    override val actions: Observable<JosekiExplorerAction>
        get() =
            Observable.merge(
                    listOf(
                            internalActions,
                            board.tapUpObservable()
                                    .map<JosekiExplorerAction>(::UserTappedCoordinate),
                            board.tapMoveObservable()
                                    .map<JosekiExplorerAction>(::UserHotTrackedCoordinate),
                            RxView.clicks(previousButton)
                                    .map<JosekiExplorerAction> { UserPressedPrevious },
                            RxView.clicks(nextButton)
                                    .map<JosekiExplorerAction> { UserPressedNext },
                            RxView.clicks(passButton)
                                    .map<JosekiExplorerAction> { UserPressedPass }
                    )
            ).startWith(ViewReady)

    override fun render(state: JosekiExplorerState) {
        currentState = state
        if(state.shouldFinish) {
            requireActivity().onBackPressed()
        }
        progressBar.showIf(state.loading)
        board.showCandidateMove(state.candidateMove, board.position?.nextToMove ?: StoneType.BLACK)
        board.drawMarks = !state.loading
        state.description?.let {
            markwon.setMarkdown(description, it)
        }
        state.boardPosition?.let {
            board.position = it
        }
        state.error?.let {
            description.text = it.message
            Log.e(TAG, it.message, it)
            Crashlytics.logException(it)
        }
        previousButton.isEnabled = state.previousButtonEnabled
        passButton.isEnabled = state.passButtonEnabled
        nextButton.isEnabled = state.nextButtonEnabled
    }

    override fun onPause() {
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)
        board.apply {
            isInteractive = true
            drawCoordinates = SettingsRepository.showCoordinates
        }
        viewModel.bind(this)
    }

    fun onBackPressed() {
        internalActions.onNext(UserPressedBack)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        PersistenceManager.visitedJosekiExplorer = true
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory {
                    JosekiExplorerViewModel(
                            Store(
                                    JosekiExplorerReducer(),
                                    listOf(
                                            LoadPositionMiddleware(),
                                            HotTrackMiddleware(),
                                            TriggerLoadingMiddleware(),
                                            AnalyticsMiddleware()
                                    ),
                                    JosekiExplorerState()
                            )
                    )
                }
        ).get(JosekiExplorerViewModel::class.java)
        (activity as? MainActivity)?.apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = "Joseki Explorer"
            chipList
            setLogoVisible(false)
            setChipsVisible(false)
        }
    }

    private fun buildMarkwon(): Markwon {
        return Markwon.builder(requireContext())
                .usePlugin(MovementMethodPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                                .linkColor(ResourcesCompat.getColor(requireContext().resources, R.color.colorPrimaryDark, requireContext().theme))
                                .headingBreakColor(0x00FF0000)
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { view, link ->
                            if (link.startsWith("Position:")) {
                                val posId = link.substring(9).toLongOrNull()
                                if (posId == null) {
                                    Log.e(TAG, "Can't resolve link $link")
                                    Crashlytics.log(Log.ERROR, TAG, "Can't resolve link $link for in the description of joseki pos ${currentState?.position?.node_id}")
                                } else {
                                    internalActions.onNext(LoadPosition(posId))
                                }
                            } else if(link.matches("\\d+".toRegex())) {
                                internalActions.onNext(LoadPosition(link.toLong()))
                            } else {
                                val uri = Uri.parse(link)
                                if(uri.host?.endsWith("online-go.com") == true && uri.path?.startsWith("/joseki/") == true) {
                                    internalActions.onNext(LoadPosition(uri.lastPathSegment?.toLong()))
                                } else {
                                    val context = view.context
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.e(TAG, "Can't resolve link $link")
                                        Crashlytics.log(Log.ERROR, TAG, "Can't resolve link $link for in the description of joseki pos ${currentState?.position?.node_id}")
                                    }
                                }
                            }
                        }
                    }
                })
                .build()
    }

    fun canHandleBack(): Boolean = currentState?.shouldFinish != true

    private inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
            }
}