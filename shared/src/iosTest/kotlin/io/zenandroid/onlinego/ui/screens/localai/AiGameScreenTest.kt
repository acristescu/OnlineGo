package io.zenandroid.onlinego.ui.screens.localai

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AiGameScreenTest {

  private val scope = CoroutineScope(SupervisorJob())

  @AfterTest
  fun tearDown() {
    stopKoin()
    scope.cancel()
  }

  @Test
  fun withoutAnEngineTheScreenSaysLocalAiIsUnavailable() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope)) }
    val viewModel = getKoin().get<AiGameViewModel>()
    val owner = object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        AiGameScreen(viewModel = viewModel, onNavigateBack = {})
      }
    }
    waitUntil(timeoutMillis = 5_000) {
      onAllNodes(hasText("Local AI is not available on iOS", substring = true))
        .fetchSemanticsNodes().isNotEmpty()
    }
    viewModel.viewModelScope.cancel()
  }
}
