package io.zenandroid.onlinego.ui.screens.mygames

import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.Warning
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.AutomatchRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.FinishedGamesRepository
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.ServerNotificationsRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.TutorialsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.Analytics
import io.zenandroid.onlinego.utils.WhatsNewUtils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MyGamesLoadingTest {
  private val session: UserSessionRepository = mock()
  private val active: ActiveGamesRepository = mock()
  private val finished: FinishedGamesRepository = mock()
  private val challenges: ChallengesRepository = mock()
  private val automatches: AutomatchRepository = mock()
  private val socket: OGSWebSocketService = mock()
  private val notifications: ServerNotificationsRepository = mock()
  private val settings: SettingsRepository = mock()
  private val rest: OGSRestService = mock()
  private val activeGames = MutableStateFlow<List<Game>>(emptyList())
  private val loginStatus = MutableSharedFlow<LoginStatus>(replay = 1)
  private lateinit var viewModel: MyGamesViewModel

  @Before
  fun setUp() = runBlocking {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    loginStatus.tryEmit(LoginStatus.LoggedIn(1L))
    whenever(session.loginStatus).thenReturn(loginStatus)
    whenever(active.monitorActiveGames()).thenReturn(activeGames)
    whenever(finished.getRecentlyFinishedGames()).thenReturn(emptyFlow())
    whenever(finished.getHistoricGames(null)).thenReturn(emptyFlow())
    whenever(challenges.monitorChallenges()).thenReturn(emptyFlow())
    whenever(automatches.automatchFlow).thenReturn(MutableStateFlow(persistentListOf()))
    whenever(automatches.gameStartFlow).thenReturn(MutableSharedFlow())
    whenever(socket.connectionState).thenReturn(MutableStateFlow(false))
    whenever(notifications.notificationsFlow()).thenReturn(emptyFlow())
    whenever(settings.showRanksFlow).thenReturn(flowOf(true))
    whenever(rest.checkForWarnings()).thenReturn(Warning())
    viewModel = MyGamesViewModel(
      session, finished, active, challenges, automatches, mock<ChatRepository>(),
      notifications, mock<TutorialsRepository>(), mock<Analytics>(), rest, socket,
      settings, mock<WhatsNewUtils>(),
    )
  }

  @After
  fun tearDown() {
    viewModel.viewModelScope.cancel()
    Dispatchers.resetMain()
  }

  @Test
  fun cachedActiveGamesAreSelectableBeforeOtherSectionsOrPreviewsLoad() = runBlocking {
    val game = Game.sampleData().copy(phase = Phase.PLAY, playerToMoveId = 1L)
    activeGames.value = listOf(game)
    val state = withTimeout(5_000) {
      viewModel.state.first { it.myTurnGames.isNotEmpty() }
    }
    assertEquals(listOf(game), state.myTurnGames)
    assertFalse(state.hasReceivedRecentGames)
    assertFalse(state.hasReceivedChallenges)
    viewModel.onAction(Action.GameSelected(state.myTurnGames.single()))
    assertSame(game, viewModel.state.value.gameNavigationPending)
  }

  @Test
  fun subsequentGameUpdatesArePublishedWhileOtherSectionsRemainPending() = runBlocking {
    val game = Game.sampleData().copy(phase = Phase.PLAY, playerToMoveId = 1L)
    activeGames.value = listOf(game)
    withTimeout(5_000) { viewModel.state.first { it.myTurnGames.isNotEmpty() } }
    activeGames.value = listOf(game.copy(playerToMoveId = 0L))
    val state = withTimeout(5_000) {
      viewModel.state.first { it.opponentTurnGames.isNotEmpty() }
    }
    assertEquals(emptyList<Game>(), state.myTurnGames)
    assertEquals(1, state.opponentTurnGames.size)
    assertFalse(state.hasReceivedRecentGames)
  }
  @Test
  fun loggingOutClearsCachedGamesAndStopsThePreviousSessionCollectors() = runBlocking {
    activeGames.value = listOf(Game.sampleData().copy(phase = Phase.PLAY))
    withTimeout(5_000) { viewModel.state.first { it.myTurnGames.isNotEmpty() } }
    loginStatus.emit(LoginStatus.LoggedOut)
    val state = withTimeout(5_000) { viewModel.state.first { it.userIsLoggedOut } }
    assertEquals(emptyList<Game>(), state.myTurnGames)
    assertEquals(emptyList<Game>(), state.opponentTurnGames)
    assertFalse(state.hasReceivedActiveGames)
  }

}
