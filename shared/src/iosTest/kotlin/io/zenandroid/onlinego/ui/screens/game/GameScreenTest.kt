package io.zenandroid.onlinego.ui.screens.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.zenandroid.onlinego.data.db.DatabaseTest
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.di.sharedModule
import io.zenandroid.onlinego.di.stubPlatformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GameScreenTest {

  private val scope = CoroutineScope(SupervisorJob())

  @AfterTest
  fun tearDown() {
    stopKoin()
    scope.cancel()
  }

  @Test
  fun gameScreenComposes() = runComposeUiTest {
    startKoin {
      modules(sharedModule, stubPlatformModule(scope))
    }
    runBlocking { getKoin().get<GameDao>().insertAllGames(listOf(DatabaseTest.game)) }
    val viewModel = getKoin().get<GameViewModel> {
      parametersOf(
        SavedStateHandle(
          mapOf<String, Any>(
            "gameId" to 1L,
            "gameWidth" to 19,
            "gameHeight" to 19
          )
        )
      )
    }
    val owner = object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }
    setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        GameScreen(viewModel = viewModel, onNavigateBack = {}, onNavigateToGameScreen = {})
      }
    }
    waitForIdle()
    check(onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty())
    viewModel.viewModelScope.cancel()
  }
}
