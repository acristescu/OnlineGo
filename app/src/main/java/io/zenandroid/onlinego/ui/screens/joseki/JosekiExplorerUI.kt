package io.zenandroid.onlinego.ui.screens.joseki

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Browser
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.BottomBar
import io.zenandroid.onlinego.ui.composables.BottomBarButton
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.recordException
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.koin.androidx.compose.koinViewModel

private const val TAG = "JosekiExplorer"

@Composable
fun JosekiExplorerScreen(
  viewModel: JosekiExplorerViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  BackHandler(enabled = state.previousButtonEnabled) {
    viewModel.onPressedPrevious()
  }
  val configuration = LocalConfiguration.current
  if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
    PortraitLayout(
      state = state,
      onTappedCoordinate = viewModel::onTappedCoordinate,
      onHotTrackedCoordinate = viewModel::onHotTrackedCoordinate,
      onLoadPosition = viewModel::loadPosition,
      onPressedPass = viewModel::onPressedPass,
      onPressedPrevious = viewModel::onPressedPrevious,
      onPressedNext = viewModel::onPressedNext,
      onNavigateBack = onNavigateBack,
    )
  } else {
    LandscapeLayout(
      state = state,
      onTappedCoordinate = viewModel::onTappedCoordinate,
      onHotTrackedCoordinate = viewModel::onHotTrackedCoordinate,
      onLoadPosition = viewModel::loadPosition,
      onPressedPass = viewModel::onPressedPass,
      onPressedPrevious = viewModel::onPressedPrevious,
      onPressedNext = viewModel::onPressedNext,
      onNavigateBack = onNavigateBack,
    )
  }
}

@Composable
private fun PortraitLayout(
  state: JosekiExplorerState,
  onTappedCoordinate: (Cell) -> Unit,
  onHotTrackedCoordinate: (Cell) -> Unit,
  onPressedPass: () -> Unit,
  onPressedPrevious: () -> Unit,
  onPressedNext: () -> Unit,
  onLoadPosition: (Long?) -> Unit,
  onNavigateBack: () -> Unit,
) {
  Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
    AppTopBar(onNavigateBack)
    LoadingIndicator(state.loading)
    DescriptionView(
      description = state.description,
      error = state.error,
      loadPosition = onLoadPosition,
      nodeId = state.lastRequestedNodeId,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    )
    BoardComponent(state, onTappedCoordinate, onHotTrackedCoordinate)
    BottomBar(
      listOf(
        Button.Tenuki(state.passButtonEnabled),
        Button.Previous(state.previousButtonEnabled),
        Button.Next(state.nextButtonEnabled)
      ),
      bottomText = null,
      onButtonPressed = {
        when (it) {
          is Button.Tenuki -> onPressedPass()
          is Button.Previous -> onPressedPrevious()
          is Button.Next -> onPressedNext()
        }
      },
    )
  }
}

@Composable
private fun LandscapeLayout(
  state: JosekiExplorerState,
  onTappedCoordinate: (Cell) -> Unit,
  onHotTrackedCoordinate: (Cell) -> Unit,
  onPressedPass: () -> Unit,
  onPressedPrevious: () -> Unit,
  onPressedNext: () -> Unit,
  onLoadPosition: (Long?) -> Unit,
  onNavigateBack: () -> Unit,
) {
  Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
    // Left side with controls
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    ) {
      AppTopBar(onNavigateBack)
      LoadingIndicator(state.loading)
      DescriptionView(
        description = state.description,
        error = state.error,
        nodeId = state.lastRequestedNodeId,
        loadPosition = onLoadPosition,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
      BottomBar(
        listOf(
          Button.Tenuki(state.passButtonEnabled),
          Button.Previous(state.previousButtonEnabled),
          Button.Next(state.nextButtonEnabled)
        ),
        bottomText = null,
        onButtonPressed = {
          when (it) {
            is Button.Tenuki -> onPressedPass()
            is Button.Previous -> onPressedPrevious()
            is Button.Next -> onPressedNext()
          }
        },
      )
    }

    BoardComponent(state, onTappedCoordinate, onHotTrackedCoordinate)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onNavigateBack: () -> Unit) {
  TopAppBar(
    title = {
      Text(
        text = "Joseki Explorer",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    navigationIcon = {
      IconButton(onClick = onNavigateBack) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    },
  )
}

@Composable
private fun LoadingIndicator(loading: Boolean) {
  if (loading) {
    LinearProgressIndicator(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
  }
}

@Composable
private fun BoardComponent(
  state: JosekiExplorerState,
  onTappedCoordinate: (Cell) -> Unit,
  onHotTrackedCoordinate: (Cell) -> Unit,
) {
  Board(
    boardWidth = 19,
    boardHeight = 19,
    position = state.boardPosition,
    interactive = !state.loading,
    candidateMove = state.candidateMove,
    onTapMove = onHotTrackedCoordinate,
    onTapUp = onTappedCoordinate,
    modifier = Modifier
      .padding(8.dp)
      .shadow(6.dp, MaterialTheme.shapes.large),
  )
}

@Composable
private fun DescriptionView(
  description: String?,
  error: Throwable?,
  nodeId: Long?,
  loadPosition: (Long?) -> Unit,
  modifier: Modifier = Modifier
) {
  val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
  val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

  val context = LocalContext.current
  val markwon = remember(context, nodeId) { buildMarkwon(context, nodeId, loadPosition) }

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

private fun buildMarkwon(context: Context, nodeId: Long?, loadPosition: (Long?) -> Unit): Markwon {
  return Markwon.builder(context)
    .usePlugin(MovementMethodPlugin.create())
    .usePlugin(object : AbstractMarkwonPlugin() {
      override fun configureTheme(builder: MarkwonTheme.Builder) {
        builder
          .linkColor(
            ResourcesCompat.getColor(
              context.resources,
              R.color.colorPrimaryDark,
              context.theme
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
              loadPosition(posId)
            }
          } else if (link.matches("\\d+".toRegex())) {
            loadPosition(link.toLong())
          } else {
            val uri = link.toUri()
            if (uri.host?.endsWith("online-go.com") == true && uri.path?.startsWith("/joseki/") == true) {
              loadPosition(uri.lastPathSegment?.toLong())
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

sealed class Button(
  override val icon: ImageVector,
  override val label: String,
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false,
) : BottomBarButton {
  class Tenuki(enabled: Boolean) : Button(
    icon = Icons.Rounded.Pause,
    label = "Tenuki",
    enabled = enabled,
  )

  class Previous(enabled: Boolean) : Button(
    icon = Icons.Rounded.SkipPrevious,
    label = "Previous",
    enabled = enabled,
  )

  class Next(enabled: Boolean) : Button(
    icon = Icons.Rounded.SkipNext,
    label = "Next",
    enabled = enabled,
  )
}

@Preview
@Composable
private fun Preview() {
  OnlineGoTheme {
    PortraitLayout(
      state = JosekiExplorerState(
        loading = false,
        description = "This is a test description with a [link](https://example.com) and a Position:1234.",
        error = null,
        lastRequestedNodeId = 1234L,
        boardPosition = null, // Replace with actual position if needed
        candidateMove = null,
        passButtonEnabled = true,
        previousButtonEnabled = true,
        nextButtonEnabled = true
      ),
      onTappedCoordinate = {},
      onHotTrackedCoordinate = {},
      onPressedPass = {},
      onPressedPrevious = {},
      onPressedNext = {},
      onLoadPosition = {},
      onNavigateBack = {}
    )
  }
}