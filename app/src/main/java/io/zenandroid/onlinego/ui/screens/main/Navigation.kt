package io.zenandroid.onlinego.ui.screens.main

import android.util.Log
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
import androidx.navigation.NavOptions.Builder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import io.zenandroid.onlinego.utils.analyticsReportScreen


@Composable
fun OnlineGoApp(
  isLoggedIn: Boolean,
  darkTheme: Boolean,
  hasCompletedOnboarding: Boolean,
) {
  val navController = rememberNavController()

  val startDestination = if (hasCompletedOnboarding) "myGames" else "onboarding"

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route
  val activity = LocalActivity.current

  val showBottomBar = currentDestination in listOf("myGames", "learn", "stats", "settings")
  var showStatsLoginPrompt by remember { mutableStateOf(false) }
  var bottomBarCollapsed by remember { mutableStateOf(false) }

  LaunchedEffect(currentDestination) {
    bottomBarCollapsed = false
  }

  LaunchedEffect(activity?.intent?.data) {
    if (activity?.intent?.data != null) {
      Log.d("OnlineGoApp", "Deep link: ${activity.intent.data}")
      FirebaseCrashlytics.getInstance().log("Deep link: ${activity.intent.data}")
    }
  }
  LaunchedEffect(currentDestination) {
    if (currentDestination != null) {
      Log.d("OnlineGoApp", "Current destination: $currentDestination")
      analyticsReportScreen(currentDestination)
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
          composable("myGames") {
            MyGamesScreen(
              onNavigateToGame = { navController.navigate("game/${it.id}/${it.width}/${it.height}") },
              onNavigateToAIGame = { navController.navigate("aiGame") },
              onNavigateToFaceToFace = { navController.navigate("faceToFace") },
              onNavigateToSupporter = { navController.navigate("supporter") },
              onNavigateToLogin = { navController.navigate("onboarding?initialPage=login") },
              onNavigateToSignUp = { navController.navigate("onboarding?initialPage=signUp") },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable("aiGame") {
            AiGameScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          composable("faceToFace") {
            FaceToFaceScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          // adb shell am start -a android.intent.action.VIEW -d "sente://game/76828314/9/9"
          composable(
            route = "game/{gameId}/{gameWidth}/{gameHeight}",
            deepLinks = listOf(
              navDeepLink {
                uriPattern = "sente://game/{gameId}/{gameWidth}/{gameHeight}"
              }
            ),
            arguments = listOf(
              navArgument("gameId") { type = NavType.LongType },
              navArgument("gameWidth") { type = NavType.IntType },
              navArgument("gameHeight") { type = NavType.IntType },
            ),
          ) { backStackEntry ->
            GameScreen(
              onNavigateBack = navController::popBackStack,
              onNavigateToGameScreen = { game ->
                navController.popBackStack()
                navController.navigate("game/${game.id}/${game.width}/${game.height}")
              })
          }

          composable("learn") {
            LearnScreen(
              onJosekiExplorer = { navController.navigate("josekiExplorer") },
              onPuzzles = { navController.navigate("puzzleDirectory") },
              onTutorial = { tutorial -> navController.navigate("tutorial/${tutorial.name}") },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable(
            "tutorial/{tutorialName}",
            arguments = listOf(navArgument("tutorialName") { type = NavType.StringType })
          ) { backStackEntry ->
            TutorialScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable("josekiExplorer") {
            JosekiExplorerScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable("settings") {
            SettingsScreen(
              onNavigateToSupport = {
                navController.navigate("supporter")
              },
              onNavigateToSocketDebug = {
                navController.navigate("socketDebug")
              },
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable("socketDebug") {
            SocketDebugScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable(
            "otherPlayerStats?playerId={playerId}",
            arguments = listOf(navArgument("playerId") { type = NavType.StringType })
          ) { backStackEntry ->
            StatsScreen()
          }

          composable("stats") {
            StatsScreen(
              onBottomBarCollapseChanged = { bottomBarCollapsed = it },
            )
          }

          composable("puzzleDirectory") {
            PuzzleDirectoryScreen(
              onNavigateBack = navController::popBackStack,
              onNavigateToPuzzle = { collectionId, puzzleId ->
                navController.navigate("tsumego/$collectionId/$puzzleId")
              }
            )
          }

          composable(
            "tsumego/{collectionId}/{puzzleId}",
            arguments = listOf(
              navArgument("collectionId") { type = NavType.LongType },
              navArgument("puzzleId") { type = NavType.LongType }
            )
          ) { backStackEntry ->
            TsumegoScreen(
              onNavigateBack = navController::popBackStack
            )
          }

          composable("supporter") {
            SupporterScreen(
              onNavigateBack = navController::popBackStack,
            )
          }

          composable(
            route = "onboarding?initialPage={initialPageArg}",
            arguments = listOf(
              navArgument("initialPageArg") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
              }
            ),
          ) { backStackEntry ->
            OnboardingScreen(
              onNavigateToMyGames = {
                navController.navigate(
                  "myGames",
                  navOptions = Builder()
                    .setPopUpTo("onboarding", inclusive = true)
                    .setLaunchSingleTop(true)
                    .build()
                )
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
          val items = listOf(
            BottomNavItem(
              "myGames",
              stringResource(R.string.bottomnavigation_botton_play),
              ImageVector.vectorResource(R.drawable.ic_board_filled)
            ),
            BottomNavItem(
              "learn",
              stringResource(R.string.bottomnavigation_botton_learn),
              ImageVector.vectorResource(R.drawable.ic_learn)
            ),
            BottomNavItem(
              "stats",
              stringResource(R.string.bottomnavigation_botton_stats),
              ImageVector.vectorResource(R.drawable.ic_diagram),
              enabled = isLoggedIn
            ),
            BottomNavItem(
              "settings",
              stringResource(R.string.bottomnavigation_botton_settings),
              ImageVector.vectorResource(R.drawable.ic_settings_filled),
            ),
          )

          val navBackStackEntry by navController.currentBackStackEntryAsState()
          val currentRoute = navBackStackEntry?.destination?.route
          val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

          SenteBottomBar(
            tabs = items,
            selectedIndex = selectedIndex,
            collapsed = bottomBarCollapsed,
            onTabSelected = {
              val target = items[it]
              if (!target.enabled) {
                showStatsLoginPrompt = true
              } else if (currentRoute != target.route) {
                navController.navigate(target.route) {
                  popUpTo("myGames") { saveState = true }
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
            navController.navigate("onboarding?initialPage=login")
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
  val route: String,
  val label: String,
  val icon: ImageVector,
  val enabled: Boolean = true
)
