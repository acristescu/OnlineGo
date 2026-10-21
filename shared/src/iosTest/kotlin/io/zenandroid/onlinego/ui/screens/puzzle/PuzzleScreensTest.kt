package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.puzzle.directory.PuzzleDirectoryScreen
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoScreen
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoViewModel
import io.zenandroid.onlinego.ui.screens.socketdebug.SocketDebugScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PuzzleScreensTest {

  private val scope = CoroutineScope(SupervisorJob())

  @BeforeTest
  fun setUp() {
    startKoin { modules(sharedModule, stubPlatformModule(scope)) }
  }

  @AfterTest
  fun tearDown() {
    stopKoin()
    scope.cancel()
  }

  @Test
  fun puzzleDirectoryShows() = runComposeUiTest {
    show { PuzzleDirectoryScreen(onNavigateBack = {}, onNavigateToPuzzle = { _, _ -> }) }
    onNodeWithText("Puzzles").assertIsDisplayed()
  }

  @Test
  fun tsumegoShows() = runComposeUiTest {
    val viewModel = getKoin().get<TsumegoViewModel> {
      parametersOf(SavedStateHandle(mapOf("collectionId" to 1L, "puzzleId" to 2L)))
    }
    show { TsumegoScreen(viewModel = viewModel, onNavigateBack = {}) }
    onNodeWithText("Tsumego").assertIsDisplayed()
  }

  @Test
  fun socketDebugShows() = runComposeUiTest {
    show { SocketDebugScreen(onNavigateBack = {}) }
    onNodeWithText("Socket Debug").assertIsDisplayed()
  }

  @Test
  fun automatchSheetShows() = runComposeUiTest {
    show {
      NewAutomatchChallengeBottomSheet(
        onDismiss = {},
        onAutomatchSearchClicked = { _, _ -> })
    }
    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("Game size").fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun ComposeUiTest.show(content: @Composable () -> Unit) {
    val owner = object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
  }
}
