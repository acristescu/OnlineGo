package io.zenandroid.onlinego.ui.screens.joseki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.rememberMarkdownState
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.back
import io.zenandroid.onlinego.resources.joseki_explorer_button_next
import io.zenandroid.onlinego.resources.joseki_explorer_button_previous
import io.zenandroid.onlinego.resources.joseki_explorer_button_tenuki
import io.zenandroid.onlinego.resources.joseki_explorer_link_external
import io.zenandroid.onlinego.resources.joseki_explorer_link_video
import io.zenandroid.onlinego.resources.joseki_explorer_title
import io.zenandroid.onlinego.ui.composables.BackHandler
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.BottomBar
import io.zenandroid.onlinego.ui.composables.BottomBarButton
import io.zenandroid.onlinego.ui.composables.isLandscape
import io.zenandroid.onlinego.ui.theme.LocalThemeSettings
import io.zenandroid.onlinego.ui.theme.OnlineGoPreviewTheme
import io.zenandroid.onlinego.ui.theme.darkBlue
import io.zenandroid.onlinego.ui.theme.nightDarkBlue
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val TAG = "JosekiExplorer"

@Composable
fun JosekiExplorerScreen(
  viewModel: JosekiExplorerViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  BackHandler(enabled = state.previousButtonEnabled) {
    viewModel.onPressedPrevious()
  }
  if (!isLandscape()) {
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
  Column(modifier = Modifier
    .background(MaterialTheme.colorScheme.surface)
    .systemBarsPadding()) {
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
  Row(modifier = Modifier
    .background(MaterialTheme.colorScheme.surface)
    .systemBarsPadding()) {
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
        text = stringResource(Res.string.joseki_explorer_title),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    navigationIcon = {
      IconButton(onClick = onNavigateBack) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(Res.string.back),
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
  description: JosekiDescription?,
  error: Throwable?,
  nodeId: Long?,
  loadPosition: (Long?) -> Unit,
  modifier: Modifier = Modifier
) {
  val markdown = when (description) {
    is JosekiDescription.Markdown -> description.markdown
    is JosekiDescription.FromResource -> stringResource(description.resId)
    null -> ""
  }
  val videoLabel = stringResource(Res.string.joseki_explorer_link_video)
  val externalLabel = stringResource(Res.string.joseki_explorer_link_external)
  val labelledMarkdown = remember(markdown, videoLabel, externalLabel) {
    labelJosekiLinks(markdown, videoLabel, externalLabel)
  }

  val platformUriHandler = LocalUriHandler.current
  val uriHandler = remember(platformUriHandler, nodeId, loadPosition) {
    JosekiUriHandler(platformUriHandler, nodeId, loadPosition)
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(8.dp)
  ) {
    if (error != null) {
      Text(
        text = error.message.orEmpty(),
        style = descriptionTextStyle(),
      )
    } else {
      CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        DescriptionMarkdown(labelledMarkdown)
      }
    }
  }
}

@Composable
private fun DescriptionMarkdown(markdown: String) {
  val flavour = remember { CommonMarkFlavourDescriptor() }
  val markdownState = rememberMarkdownState(markdown, retainState = true, flavour = flavour)
  val text = descriptionTextStyle()
  val linkColor = if (LocalThemeSettings.current.isDarkTheme) nightDarkBlue else darkBlue
  val heading =
    { scale: Float -> text.copy(fontSize = text.fontSize * scale, fontWeight = FontWeight.Bold) }
  Markdown(
    markdownState = markdownState,
    colors = markdownColor(text = text.color),
    typography = markdownTypography(
      h1 = heading(2f),
      h2 = heading(1.5f),
      h3 = heading(1.17f),
      h4 = heading(1f),
      h5 = heading(.83f),
      h6 = heading(.67f),
      text = text,
      paragraph = text,
      ordered = text,
      bullet = text,
      list = text,
      quote = text.copy(fontStyle = FontStyle.Italic),
      textLink = TextLinkStyles(
        SpanStyle(
          color = linkColor,
          textDecoration = TextDecoration.Underline
        )
      ),
    ),
    animations = markdownAnimations(animateTextSize = { this }),
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun descriptionTextStyle() =
  MaterialTheme.typography.bodyMedium.copy(
    fontSize = 14.sp,
    color = MaterialTheme.colorScheme.onSurface
  )

private class JosekiUriHandler(
  private val platformUriHandler: UriHandler,
  private val nodeId: Long?,
  private val loadPosition: (Long?) -> Unit,
) : UriHandler {
  override fun openUri(uri: String) {
    val link = josekiLinkOf(uri)
    if (link is JosekiLink.Position) {
      if (link.nodeId != null) {
        loadPosition(link.nodeId)
      } else {
        Logger.e("Can't resolve link $uri in the description of joseki pos $nodeId", tag = TAG)
      }
    } else {
      try {
        platformUriHandler.openUri(uri)
      } catch (_: IllegalArgumentException) {
        Logger.e("Can't open link $uri in the description of joseki pos $nodeId", tag = TAG)
      }
    }
  }
}

sealed class Button(
  override val icon: ImageVector,
  override val labelResId: StringResource,
  override val label: String = "",
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false,
) : BottomBarButton {
  class Tenuki(enabled: Boolean) : Button(
    icon = Icons.Rounded.Pause,
    labelResId = Res.string.joseki_explorer_button_tenuki,
    enabled = enabled,
  )

  class Previous(enabled: Boolean) : Button(
    icon = Icons.Rounded.SkipPrevious,
    labelResId = Res.string.joseki_explorer_button_previous,
    enabled = enabled,
  )

  class Next(enabled: Boolean) : Button(
    icon = Icons.Rounded.SkipNext,
    labelResId = Res.string.joseki_explorer_button_next,
    enabled = enabled,
  )
}

@Preview
@Composable
private fun Preview() {
  OnlineGoPreviewTheme {
    PortraitLayout(
      state = JosekiExplorerState(
        loading = false,
        description = JosekiDescription.Markdown(
          "This is a test description with a [link](https://example.com) and a Position:1234."
        ),
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