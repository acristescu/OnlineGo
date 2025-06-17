package io.zenandroid.onlinego.ui.screens.joseki

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.recordException
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TAG = "JosekiExplorerFragment"

class JosekiExplorerFragment : Fragment() {
  private val settingsRepository: SettingsRepository by inject()
  private val viewModel: JosekiExplorerViewModel by viewModel()

  private val markwon by lazy { buildMarkwon() }
  private var nodeId: Long? = null

  private val onBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      viewModel.onPressedBack()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by viewModel.state.collectAsState()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (state.shouldFinish) {
          onBackPressedCallback.isEnabled = false
          requireActivity().onBackPressed()
        }

        OnlineGoTheme {
          nodeId = state.position?.node_id

          if (isLandscape) {
            LandscapeLayout(state)
          } else {
            PortraitLayout(state)
          }
        }
      }
    }
  }

  @Composable
  private fun PortraitLayout(state: JosekiExplorerState) {
    Column(modifier = Modifier.background(MaterialTheme.colors.surface)) {
      AppTopBar()
      LoadingIndicator(state.loading)
      DescriptionView(
        description = state.description,
        error = state.error,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
      BoardComponent(state)
      BottomBar(state)
    }
  }

  @Composable
  private fun LandscapeLayout(state: JosekiExplorerState) {
    Row(modifier = Modifier.background(MaterialTheme.colors.surface)) {
      // Left side with controls
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
        AppTopBar()
        LoadingIndicator(state.loading)
        DescriptionView(
          description = state.description,
          error = state.error,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        )
        BottomBar(state)
      }
      
      // Right side with board
      BoardComponent(state)
    }
  }

  @Composable
  private fun AppTopBar() {
    TopAppBar(
      title = {
        Text(
          text = "Joseki Explorer",
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colors.onSurface
        )
      },
      navigationIcon = {
        IconButton(onClick = {
          onBackPressedCallback.isEnabled = false
          requireActivity().onBackPressed()
        }) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colors.onSurface
          )
        }
      },
      elevation = 1.dp,
      backgroundColor = MaterialTheme.colors.surface
    )
  }

  @Composable
  private fun LoadingIndicator(loading: Boolean) {
    if (loading) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp),
        color = MaterialTheme.colors.primary,
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
      )
    }
  }

  @Composable
  private fun BoardComponent(state: JosekiExplorerState) {
    Board(
      boardWidth = 19,
      boardHeight = 19,
      boardTheme = settingsRepository.boardTheme,
      position = state.boardPosition,
      interactive = !state.loading,
      candidateMove = state.candidateMove,
      onTapMove = viewModel::onHotTrackedCoordinate,
      onTapUp = viewModel::onTappedCoordinate,
      modifier = Modifier
        .padding(8.dp)
        .shadow(6.dp, MaterialTheme.shapes.large),
    )
  }

  @Composable
  private fun DescriptionView(
    description: String?,
    error: Throwable?,
    modifier: Modifier = Modifier
  ) {
    val textColor = MaterialTheme.colors.onSurface.toArgb()
    val backgroundColor = MaterialTheme.colors.surface.toArgb()

    AndroidView(
      factory = { context ->
        TextView(context).apply {
          setPadding(16, 16, 16, 16)
          setTextColor(textColor)
          setBackgroundColor(backgroundColor)
          textSize = 14f
          isVerticalScrollBarEnabled = true
          scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        }
      },
      update = { textView ->
        when {
          error != null -> {
            textView.text = error.message
            Log.e(TAG, error.message ?: "Unknown error", error)
            recordException(error)
          }

          description != null -> {
            markwon.setMarkdown(textView, description)
          }

          else -> {
            textView.text = ""
          }
        }
      },
      modifier = modifier
    )
  }

  @Composable
  private fun BottomBar(state: JosekiExplorerState) {
    Row(modifier = Modifier.height(56.dp)) {
      BottomBarButton(
        icon = Icons.Rounded.Pause,
        label = "Tenuki",
        enabled = state.passButtonEnabled,
        onButtonPressed = viewModel::onPressedPass,
      )
      BottomBarButton(
        icon = Icons.Rounded.SkipPrevious,
        label = "Previous",
        enabled = state.previousButtonEnabled,
        onButtonPressed = viewModel::onPressedPrevious,
      )
      BottomBarButton(
        icon = Icons.Rounded.SkipNext,
        label = "Next",
        enabled = state.nextButtonEnabled,
        onButtonPressed = viewModel::onPressedNext,
      )
    }
  }

  @Composable
  private fun RowScope.BottomBarButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onButtonPressed: () -> Unit,
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .weight(1f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .alpha(if (enabled) 1f else .4f)
          .clickable(enabled = enabled) {
            onButtonPressed()
          },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          icon,
          null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colors.onSurface,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.h5,
          color = MaterialTheme.colors.onSurface,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("Joseki Explorer")
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
  }

  private fun buildMarkwon(): Markwon {
    return Markwon.builder(requireContext())
      .usePlugin(MovementMethodPlugin.create())
      .usePlugin(object : AbstractMarkwonPlugin() {
        override fun configureTheme(builder: MarkwonTheme.Builder) {
          builder
            .linkColor(
              ResourcesCompat.getColor(
                requireContext().resources,
                R.color.colorPrimaryDark,
                requireContext().theme
              )
            )
            .headingBreakColor(0x00FF0000)
        }

        override fun beforeRender(node: Node) {
          node.accept(object : AbstractVisitor() {
            override fun visit(link: Link) {
              val uri = link.destination.toUri()
              when {
                link.destination.startsWith("Position:")
                    || link.destination.matches("\\d+".toRegex())
                    || (uri.host?.endsWith("online-go.com") == true && uri.path?.startsWith("/joseki/") == true) -> {
                }

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
                FirebaseCrashlytics.getInstance()
                  .log("E/$TAG: Can't resolve link $link for in the description of joseki pos $nodeId")
              } else {
                viewModel.loadPosition(posId)
              }
            } else if (link.matches("\\d+".toRegex())) {
              viewModel.loadPosition(link.toLong())
            } else {
              val uri = link.toUri()
              if (uri.host?.endsWith("online-go.com") == true && uri.path?.startsWith("/joseki/") == true) {
                viewModel.loadPosition(uri.lastPathSegment?.toLong())
              } else {
                val context = view.context
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                try {
                  context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                  Log.e(TAG, "Can't resolve link $link")
                  FirebaseCrashlytics.getInstance()
                    .log("E/$TAG: Can't resolve link $link for in the description of joseki pos $nodeId")
                }
              }
            }
          }
        }
      })
      .build()
  }
}