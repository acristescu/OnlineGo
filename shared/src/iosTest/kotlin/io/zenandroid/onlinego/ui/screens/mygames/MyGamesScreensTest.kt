package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.newchallenge.SelectOpponentDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MyGamesScreensTest {

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
  fun myGamesComposes() = runComposeUiTest {
    show {
      MyGamesScreen(
        onNavigateToGame = {},
        onNavigateToAIGame = {},
        onNavigateToFaceToFace = {},
        onNavigateToSupporter = {},
        onNavigateToLogin = {},
        onNavigateToSignUp = {},
      )
    }
    waitForIdle()
    check(onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty())
  }

  @Test
  fun newChallengeSheetShows() = runComposeUiTest {
    show { NewChallengeBottomSheet(onDismiss = {}, onNewChallengeSearchClicked = {}) }
    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("Board Size").fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun opponentPickerComposesWithItsBackHandler() = runComposeUiTest {
    show { SelectOpponentDialog(onDialogDismiss = {}) }
    waitForIdle()
    check(onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty())
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
