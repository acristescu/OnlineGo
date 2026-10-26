package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import io.zenandroid.onlinego.utils.ReviewDiagnosticResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnlineGoAppTest {

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
  fun theBottomBarSwitchesTabsAndAsksLoggedOutUsersToLogInForStats() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope, TUTORIALS)) }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        OnlineGoApp(
          isLoggedIn = false,
          darkTheme = false,
          hasCompletedOnboarding = true,
          versionCode = 1,
          onForceReviewPrompt = { ReviewDiagnosticResult(true, emptyList(), null) },
          onTestNotification = {},
          onLaunchBillingFlow = { _, _ -> },
          onFinish = {},
        )
      }
    }

    onNodeWithText("Learn").performClick()
    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("The Basics").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("The Basics").assertIsDisplayed()

    onNodeWithText("Stats").performClick()
    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("Log in to view stats").fetchSemanticsNodes().isNotEmpty()
    }
  }
}

private const val TUTORIALS = """
[
  {
    "name": "tutorial_basics_title",
    "icon": "BEGINNER",
    "tutorials": [{ "name": "tutorial_basics_thebasics_title", "steps": [] }]
  }
]
"""
