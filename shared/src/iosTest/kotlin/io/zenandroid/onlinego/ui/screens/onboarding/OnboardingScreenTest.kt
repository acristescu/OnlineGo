package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
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
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnboardingScreenTest {

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
  fun googleSignInReturnsToTheLoginMethodsUntilIosSupportsIt() = runComposeUiTest {
    startKoin { modules(sharedModule, stubPlatformModule(scope)) }
    val viewModel = getKoin().get<OnboardingViewModel> {
      parametersOf(SavedStateHandle(mapOf("initialPage" to "login")))
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        OnboardingScreen(viewModel = viewModel, onNavigateToMyGames = {}, onNavigateBack = {})
      }
    }

    onNodeWithText("What log in method do you want to use?").assertIsDisplayed()
    onNodeWithText("Google Sign-in").performClick()

    waitUntil(timeoutMillis = 5_000) {
      onAllNodesWithText("What log in method do you want to use?").fetchSemanticsNodes()
        .isNotEmpty()
    }
    onNodeWithText("Google Sign-in").assertIsDisplayed()
  }
}
