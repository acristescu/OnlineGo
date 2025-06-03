package io.zenandroid.onlinego.ui.screens.joseki

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.rxbinding3.view.clicks
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.FragmentJosekiBinding
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.LoadPosition
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserHotTrackedCoordinate
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserPressedBack
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserPressedNext
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserPressedPass
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserPressedPrevious
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.UserTappedCoordinate
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction.ViewReady
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.showIf
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TAG = "JosekiExplorerFragment"

class JosekiExplorerFragment : Fragment(), MviView<JosekiExplorerState, JosekiExplorerAction> {
    private val settingsRepository: SettingsRepository by inject()
    private val viewModel: JosekiExplorerViewModel by viewModel()

    private val markwon by lazy { buildMarkwon() }

    private val internalActions = PublishSubject.create<JosekiExplorerAction>()
    private var currentState: JosekiExplorerState? = null
    private lateinit var binding: FragmentJosekiBinding

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            internalActions.onNext(UserPressedBack)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentJosekiBinding.inflate(inflater, container, false)
        binding.backArrow?.setOnClickListener { findNavController().navigateUp() }
        return binding.root
    }

    override val actions: Observable<JosekiExplorerAction>
        get() =
            Observable.merge(
                    listOf(
                            internalActions,
                            binding.board.tapUpObservable()
                                    .map<JosekiExplorerAction>(::UserTappedCoordinate),
                            binding.board.tapMoveObservable()
                                    .map<JosekiExplorerAction>(::UserHotTrackedCoordinate),
                            binding.previousButton.clicks()
                                    .map<JosekiExplorerAction> { UserPressedPrevious },
                            binding.nextButton.clicks()
                                    .map<JosekiExplorerAction> { UserPressedNext },
                            binding.passButton.clicks()
                                    .map<JosekiExplorerAction> { UserPressedPass }
                    )
            ).startWith(ViewReady)

    override fun render(state: JosekiExplorerState) {
        currentState = state
        if(state.shouldFinish) {
            onBackPressedCallback.isEnabled = false
            requireActivity().onBackPressed()
        }
        binding.progressBar.showIf(state.loading)
        binding.board.showCandidateMove(state.candidateMove, binding.board.position?.nextToMove ?: StoneType.BLACK)
        binding.board.drawMarks = !state.loading
        state.description?.let {
            markwon.setMarkdown(binding.description, it)
        }
        state.boardPosition?.let {
            binding.board.position = it
        }
        state.error?.let {
            binding.description.text = it.message
            Log.e(TAG, it.message, it)
            recordException(it)
        }
        binding.previousButton.isEnabled = state.previousButtonEnabled
        binding.passButton.isEnabled = state.passButtonEnabled
        binding.nextButton.isEnabled = state.nextButtonEnabled
    }

    override fun onPause() {
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Joseki Explorer")
        binding.board.apply {
            isInteractive = true
            drawCoordinates = settingsRepository.showCoordinates
        }
        viewModel.bind(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        PersistenceManager.visitedJosekiExplorer = true
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

                    override fun beforeRender(node: Node) {
                        node.accept(object : AbstractVisitor() {
                            override fun visit(link: Link) {
                                val uri = Uri.parse(link.destination)
                                when {
                                    link.destination.startsWith("Position:")
                                            || link.destination.matches("\\d+".toRegex())
                                            || (uri.host?.endsWith("online-go.com") == true && uri.path?.startsWith("/joseki/") == true) -> {}
                                    uri.host == "youtube.com" || uri.host == "youtu.be" || uri.host == "www.youtube.com" -> {
                                        link.appendChild(Text(" (video)"))
                                    }
                                    else -> {
                                        link.appendChild(Text(" (external link)"))
                                    }
                                }
                            }
                        })
                        super.beforeRender(node)
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { view, link ->
                            if (link.startsWith("Position:")) {
                                val posId = link.substring(9).toLongOrNull()
                                if (posId == null) {
                                    Log.e(TAG, "Can't resolve link $link")
                                    FirebaseCrashlytics.getInstance().log("E/$TAG: Can't resolve link $link for in the description of joseki pos ${currentState?.position?.node_id}")
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
                                        FirebaseCrashlytics.getInstance().log("E/$TAG: Can't resolve link $link for in the description of joseki pos ${currentState?.position?.node_id}")
                                    }
                                }
                            }
                        }
                    }
                })
                .build()
    }
}