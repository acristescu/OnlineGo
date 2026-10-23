package io.zenandroid.onlinego.ui.screens.joseki

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class JosekiExplorerScreenTest {

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
  fun rendersTheDescriptionAsMarkdownWithLinkLabels() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope)) }
    runBlocking {
      getKoin().get<GameDao>().insertJosekiPositionsReplacingDuplicates(
        listOf(
          JosekiPosition(
            node_id = 1,
            play = ".root",
            placement = "Q16",
            description = "## A corner\n\nSee [this video](https://youtu.be/x).",
          )
        )
      )
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        JosekiExplorerScreen(onNavigateBack = {})
      }
    }

    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("A corner").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("A corner").assertIsDisplayed()
    onNodeWithText("See this video (video).").assertIsDisplayed()
  }
}
