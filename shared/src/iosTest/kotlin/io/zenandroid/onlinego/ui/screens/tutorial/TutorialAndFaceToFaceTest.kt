package io.zenandroid.onlinego.ui.screens.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceScreen
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
class TutorialAndFaceToFaceTest {

  private val scope = CoroutineScope(SupervisorJob())

  @BeforeTest
  fun setUp() {
    startKoin { modules(sharedModule, stubPlatformModule(scope, TUTORIALS)) }
  }

  @AfterTest
  fun tearDown() {
    stopKoin()
    scope.cancel()
  }

  @Test
  fun tutorialShowsItsTitle() = runComposeUiTest {
    val viewModel = getKoin().get<TutorialViewModel> {
      parametersOf(SavedStateHandle(mapOf("tutorialName" to "tutorial_basics_thebasics_title")))
    }
    show { TutorialScreen(viewModel = viewModel, onNavigateBack = {}) }
    waitForText("The Basics")
  }

  @Test
  fun faceToFaceShowsBothPlayers() = runComposeUiTest {
    show { FaceToFaceScreen(onNavigateBack = {}) }
    waitForText("Player 1")
    waitForText("Player 2")
  }

  private fun ComposeUiTest.show(content: @Composable () -> Unit) {
    val owner = object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
  }

  private fun ComposeUiTest.waitForText(text: String) =
    waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
}

private const val TUTORIALS = """
[
  {
    "name": "tutorial_basics_title",
    "icon": "BEGINNER",
    "tutorials": [
      {
        "name": "tutorial_basics_thebasics_title",
        "steps": [
          { "type": "Lesson", "name": "capture1", "size": "9", "pages": [{ "text": "tutorial_basics_thebasics_capture1_page1_text", "position": "B[fe]W[ee]" }] }
        ]
      }
    ]
  }
]
"""
