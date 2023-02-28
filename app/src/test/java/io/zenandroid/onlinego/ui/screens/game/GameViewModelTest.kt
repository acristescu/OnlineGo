package io.zenandroid.onlinego.ui.screens.game

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.molecule.RecompositionClock
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.usecases.GetUserStatsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule
import org.mockito.Mockito

class GameViewModelTest {

  @get:Rule
  val koinTestRule = KoinTestRule.create {
    printLogger(Level.DEBUG)
    modules(allKoinModules)
  }

  @get:Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    Mockito.`when`(settingsRepository.boardTheme).thenReturn(BoardTheme.WOOD)
    Mockito.`when`(activeGamesRepository.monitorGameFlow(any())).thenReturn(gameFlow)
    Mockito.`when`(chatRepository.monitorGameChat(any())).thenReturn(flow{})
  }

  private val userSessionRepository: UserSessionRepository = mock {}
  private val activeGamesRepository: ActiveGamesRepository = mock {}
  private val clockDriftRepository: ClockDriftRepository = mock {}
  private val chatRepository: ChatRepository = mock {}
  private val settingsRepository: SettingsRepository = mock()
  private val socketService: OGSWebSocketService = mock {}
  private val getUserStatsUseCase: GetUserStatsUseCase = mock {}

  private val gameFlow = MutableSharedFlow<Game>(extraBufferCapacity = 1)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun koDialogIsShown() {
    Dispatchers.setMain(StandardTestDispatcher())
    runTest {
      val viewModel = GameViewModel(
        activeGamesRepository,
        userSessionRepository,
        clockDriftRepository,
        socketService,
        chatRepository,
        settingsRepository,
        getUserStatsUseCase,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
      )
      viewModel.initialize(1, 19, 19)
      runCurrent()

      Assert.assertEquals(true, viewModel.state.value.loading)
      gameFlow.emit(Game.sampleData())
      gameFlow.emit(Game.sampleData())
      gameFlow.emit(Game.sampleData())
      gameFlow.emit(Game.sampleData())
      gameFlow.emit(Game.sampleData())
      gameFlow.emit(Game.sampleData())
      runCurrent()
      advanceUntilIdle()
      Assert.assertEquals(false, viewModel.state.value.loading)

      println(viewModel.state.value.toString())
    }
    Dispatchers.resetMain()
  }
}