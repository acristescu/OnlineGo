package io.zenandroid.onlinego.ui.screens.learn

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LearnScreenTest {

  private val scope = CoroutineScope(SupervisorJob())

  @AfterTest
  fun tearDown() {
    stopKoin()
    scope.cancel()
  }

  @Test
  fun showsTheTutorialGroupsThroughTheSharedViewModel() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope, TUTORIALS)) }
    val owner = object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        LearnScreen(onJosekiExplorer = {}, onPuzzles = {}, onTutorial = {})
      }
    }

    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("The Basics").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Basics").assertIsDisplayed()
    onNodeWithText("The Basics").assertIsDisplayed()
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
