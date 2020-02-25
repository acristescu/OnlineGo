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
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.mvi.Store
import io.zenandroid.onlinego.settings.SettingsRepository
import kotlinx.android.synthetic.main.fragment_joseki.*

private const val TAG = "JosekiExplorerFragment"

class JosekiExplorerFragment : Fragment(R.layout.fragment_joseki), MviView<JosekiExplorerAction, JosekiExplorerState> {
    private lateinit var viewModel: JosekiExplorerViewModel
    private val markwon by lazy {
        Markwon.builder(requireContext())
                .usePlugin(MovementMethodPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                                .linkColor(ResourcesCompat.getColor(requireContext().resources, R.color.colorPrimaryDark, requireContext().theme))
                                .headingBreakColor(0x00FF0000)
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { view, link ->
                            if(link.startsWith("Position:")) {
                                val posId = link.substring(9).toLongOrNull()
                                if(posId == null) {
                                    Log.e(TAG, "Can't resolve link $link")
                                    Crashlytics.log(Log.ERROR, TAG, "Can't resolve link $link")
                                } else {
                                    internalActions.onNext(LoadPosition(posId))
                                }
                            } else {
                                val uri = Uri.parse(link)
                                val context = view.context
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    Log.e(TAG, "Can't resolve link $link")
                                    Crashlytics.log(Log.ERROR, TAG, "Can't resolve link $link")
                                }
                            }
                        }
                    }
                })
                .build()
    }

    private val internalActions = PublishSubject.create<JosekiExplorerAction>()

    override val actions: Observable<JosekiExplorerAction>
        get() =
            Observable.merge(
                    internalActions,
                    board.tapUpObservable()
                        .map<JosekiExplorerAction>(::UserTappedCoordinate),
                    board.tapMoveObservable()
                        .map<JosekiExplorerAction>(::UserHotTrackedCoordinate)
            )
                .startWith(ViewReady)

    override fun render(state: JosekiExplorerState) {
        progressBar.showIf(state.loading)
        board.showCandidateMove(state.candidateMove, board.position?.nextToMove ?: StoneType.BLACK)
        board.drawMarks = !state.loading
        state.position?.description?.let {
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
    }

    override fun onPause() {
        viewModel.unbind()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        board.apply {
            isInteractive = true
            drawCoordinates = SettingsRepository.showCoordinates
        }
        viewModel.bind(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory {
                    JosekiExplorerViewModel(
                            Store(
                                    JosekiExplorerReducer(),
                                    listOf(
                                            LoadPositionMiddleware(),
                                            HotTrackMiddleware(),
                                            TriggerLoadingMiddleware()
                                    ),
                                    JosekiExplorerState()
                            )
                    )
                }
        ).get(JosekiExplorerViewModel::class.java)

    }

    private inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
            }
}