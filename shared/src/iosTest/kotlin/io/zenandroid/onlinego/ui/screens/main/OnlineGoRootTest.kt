package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import io.zenandroid.onlinego.utils.ReviewDiagnosticResult
import io.zenandroid.onlinego.utils.ReviewRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnlineGoRootTest {

  private val scope = CoroutineScope(SupervisorJob())
  private val owner = object : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
  }

  @AfterTest
  fun tearDown() {
    owner.viewModelStore.clear()
    stopKoin()
    scope.cancel()
  }

  @Test
  fun aFirstStartLandsOnOnboarding() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope)) }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        OnlineGoRoot(
          versionCode = 1,
          reviewRequester = ReviewRequester {},
          onForceReviewPrompt = { ReviewDiagnosticResult(true, emptyList(), null) },
          onTestNotification = {},
          onLaunchBillingFlow = { _, _ -> },
          onFinish = {},
        )
      }
    }

    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("The Game of GO").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("The Game of GO").assertIsDisplayed()
  }
}
