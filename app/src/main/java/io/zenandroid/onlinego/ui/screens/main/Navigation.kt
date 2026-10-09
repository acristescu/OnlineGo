package io.zenandroid.onlinego.ui.screens.main

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.composables.SenteBottomBar
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceScreen
import io.zenandroid.onlinego.ui.screens.game.GameScreen
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerScreen
import io.zenandroid.onlinego.ui.screens.learn.LearnScreen
import io.zenandroid.onlinego.ui.screens.localai.AiGameScreen
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesScreen
import io.zenandroid.onlinego.ui.screens.onboarding.OnboardingScreen
import io.zenandroid.onlinego.ui.screens.puzzle.directory.PuzzleDirectoryScreen
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoScreen
import io.zenandroid.onlinego.ui.screens.settings.SettingsScreen
import io.zenandroid.onlinego.ui.screens.socketdebug.SocketDebugScreen
import io.zenandroid.onlinego.ui.screens.stats.StatsScreen
import io.zenandroid.onlinego.ui.screens.supporter.SupporterScreen
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialScreen
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.Analytics
import org.koin.compose.koinInject


@Composable
fun OnlineGoApp(
  isLoggedIn: Boolean,
  darkTheme: Boolean,
  hasCompletedOnboarding: Boolean,
) {
  val navController = rememberNavController()

  val startDestination: Route = if (hasCompletedOnboarding) Route.MyGames else Route.Onboarding()

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val destination = navBackStackEntry?.destination
  val currentDestination = destination?.route
  val activity = LocalActivity.current
  val analytics: Analytics = koinInject()

  val bottomNavItems = listOf(
    BottomNavItem(
      Route.MyGames,
      stringResource(R.string.bottomnavigation_botton_play),
      ImageVector.vectorResource(R.drawable.ic_board_filled)
    ),
    BottomNavItem(
      Route.Learn,
      stringResource(R.string.bottomnavigation_botton_learn),
      ImageVector.vectorResource(R.drawable.ic_learn)
    ),
    BottomNavItem(
      Route.Stats,
      stringResource(R.string.bottomnavigation_botton_stats),
      ImageVector.vectorResource(R.drawable.ic_diagram),
      enabled = isLoggedIn
    ),
    BottomNavItem(
      Route.Settings,
      stringResource(R.string.bottomnavigation_botton_settings),
      ImageVector.vectorResource(R.drawable.ic_settings_filled),
    ),
  )
  val selectedTabIndex =
    bottomNavItems.indexOfFirst { destination?.hasRoute(it.route::class) == true }
  val showBottomBar = selectedTabIndex >= 0
  var showStatsLoginPrompt by remember { mutableStateOf(false) }
  var bottomBarCollapsed by remember { mutableStateOf(false) }

  LaunchedEffect(currentDestination) {
    bottomBarCollapsed = false
  }

  LaunchedEffect(activity?.intent?.data) {
    if (activity?.intent?.data != null) {
      Logger.i("Deep link: ${activity.intent.data}", tag = "OnlineGoApp")
    }
  }
  LaunchedEffect(currentDestination) {
    if (currentDestination != null) {
      Logger.d(tag = "OnlineGoApp") { "Current destination: $currentDestination" }
      analytics.logScreenView(currentDestination)
    }
  }

  OnlineGoTheme(
    darkTheme = darkTheme,
  ) {
    Scaffold { innerPadding ->
      Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
          navController = navController,
          startDestination = startDestination,
          enterTransition = { fadeIn(animationSpec = tween(500)) },
          exitTransition = { fadeOut(animationSpec = tween(500)) },
          popEnterTransition = { fadeIn(animationSpec = tween(500)) },
          popExitTransition = { fadeOut(animationSpec = tween(500)) },
        ) {
          composable<Route.MyGames> {
            MyGamesScreen(
              onNavigateToGame = { navController.navigate(Route.Game(it.id, it.width, it.height)) },
              onNavigateToAIGame = { navController.navigate(Route.AiGame) },
              onNavigateToFaceToFace = { navController.navigate(Route.FaceToFace) },
              onNavigateToSupporter = { navController.navigate(Route.Supporter) },
              onNavigateToLogin = { navController.navigate(Route.Onboarding(initialPage = "login")) },
              onNavigateToSignUp = { navController.navigate(Route.Onboarding(initialPage = "signUp")) },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable<Route.AiGame> {
            AiGameScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          composable<Route.FaceToFace> {
            FaceToFaceScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          // adb shell am start -a android.intent.action.VIEW -d "sente://game/76828314/9/9"
          composable<Route.Game>(
            deepLinks = listOf(navDeepLink<Route.Game>(basePath = "sente://game")),
          ) {
            GameScreen(
              onNavigateBack = navController::popBackStack,
              onNavigateToGameScreen = { game ->
                navController.popBackStack()
                navController.navigate(Route.Game(game.id, game.width, game.height))
              })
          }

          composable<Route.Learn> {
            LearnScreen(
              onJosekiExplorer = { navController.navigate(Route.JosekiExplorer) },
              onPuzzles = { navController.navigate(Route.PuzzleDirectory) },
              onTutorial = { tutorial -> navController.navigate(Route.Tutorial(tutorial.name)) },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable<Route.Tutorial> {
            TutorialScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable<Route.JosekiExplorer> {
            JosekiExplorerScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable<Route.Settings> {
            SettingsScreen(
              onNavigateToSupport = {
                navController.navigate(Route.Supporter)
              },
              onNavigateToSocketDebug = {
                navController.navigate(Route.SocketDebug)
              },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable<Route.SocketDebug> {
            SocketDebugScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable<Route.OtherPlayerStats> {
            StatsScreen()
          }

          composable<Route.Stats> {
            StatsScreen(
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable<Route.PuzzleDirectory> {
            PuzzleDirectoryScreen(
              onNavigateBack = navController::popBackStack,
              onNavigateToPuzzle = { collectionId, puzzleId ->
                navController.navigate(Route.Tsumego(collectionId, puzzleId))
              }
            )
          }

          composable<Route.Tsumego> {
            TsumegoScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable<Route.Supporter> {
            SupporterScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          composable<Route.Onboarding> {
            OnboardingScreen(
              onNavigateToMyGames = {
                navController.navigate(Route.MyGames) {
                  popUpTo<Route.Onboarding> { inclusive = true }
                  launchSingleTop = true
                }
              },
              onNavigateBack = {
                val success = navController.popBackStack()
                if (!success) {
                  activity?.finish()
                }
              },
            )
          }
        }
        if (showBottomBar) {
          SenteBottomBar(
            tabs = bottomNavItems,
            selectedIndex = selectedTabIndex,
            collapsed = bottomBarCollapsed,
            onTabSelected = {
              val target = bottomNavItems[it]
              if (!target.enabled) {
                showStatsLoginPrompt = true
              } else if (it != selectedTabIndex) {
                navController.navigate(target.route) {
                  popUpTo<Route.MyGames> { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
            modifier = Modifier
              .align(Alignment.BottomCenter)
          )
          val bottomGradient = Brush.verticalGradient(
            0f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            0.25f to MaterialTheme.colorScheme.surface.copy(alpha = .5f),
            1f to MaterialTheme.colorScheme.surface.copy(alpha = 1f)
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter)
              .background(bottomGradient)
              .height(innerPadding.calculateBottomPadding() + 24.dp)
          )
          val topGradient = Brush.verticalGradient(
            0f to MaterialTheme.colorScheme.surface.copy(alpha = .8f),
            0.75f to MaterialTheme.colorScheme.surface.copy(alpha = .5f),
            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0f)
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(topGradient)
              .statusBarsPadding()
          )
        }
      }
      if (showStatsLoginPrompt) {
        StatsLoginRequiredBottomSheet(
          onLogIn = {
            showStatsLoginPrompt = false
            navController.navigate(Route.Onboarding(initialPage = "login"))
          },
          onDismiss = { showStatsLoginPrompt = false },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsLoginRequiredBottomSheet(
  onLogIn: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    sheetState = rememberModalBottomSheetState(true),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Column(
      modifier = Modifier
        .navigationBarsPadding()
        .padding(horizontal = 24.dp)
        .padding(bottom = 16.dp)
    ) {
      Text(
        text = stringResource(R.string.stats_login_required_title),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.stats_login_required_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
      )
      Button(
        onClick = onLogIn,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.home_button_login_ogs))
      }
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
      ) {
        Text(stringResource(R.string.cancel))
      }
    }
  }
}

@Immutable
data class BottomNavItem(
  val route: Route,
  val label: String,
  val icon: ImageVector,
  val enabled: Boolean = true
)
