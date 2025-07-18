package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavOptions.Builder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.zenandroid.onlinego.R
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
import io.zenandroid.onlinego.ui.screens.stats.StatsScreen
import io.zenandroid.onlinego.ui.screens.supporter.SupporterScreen
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialScreen
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun OnlineGoApp(
  isLoggedIn: Boolean,
  darkTheme: Boolean,
  hasCompletedOnboarding: Boolean,
  onAppReady: () -> Unit,
) {
  val navController = rememberNavController()

  val startDestination = if (hasCompletedOnboarding || isLoggedIn) "myGames" else "onboarding"

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route

  val showBottomBar = currentDestination in listOf("myGames", "learn", "stats", "settings")

  LaunchedEffect(navBackStackEntry) {
    withContext(Dispatchers.IO) {
      navBackStackEntry?.destination?.route?.let { route ->
        analyticsReportScreen(route)
      }
    }
  }

  OnlineGoTheme(
    m3 = true,
    darkTheme = darkTheme,
  ) {
    Scaffold(bottomBar = {
      AnimatedVisibility(
        showBottomBar,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + expandVertically(),
        exit = ExitTransition.None,
      ) {
        BottomNavigationBar(navController, isLoggedIn)
      }
    }) { innerPadding ->
      NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        modifier = Modifier
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
      ) {
        composable("myGames") {
          MyGamesScreen(
            onScreenReady = onAppReady,
            onNavigateToGame = { navController.navigate("game/${it.id}/${it.width}/${it.height}") },
            onNavigateToAIGame = { navController.navigate("aiGame") },
            onNavigateToFaceToFace = { navController.navigate("faceToFace") },
            onNavigateToSupporter = { navController.navigate("supporter") },
            onNavigateToLogin = { navController.navigate("onboarding?initialPage=login") },
            onNavigateToSignUp = { navController.navigate("onboarding?initialPage=signUp") },
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
            onTutorial = { tutorial -> navController.navigate("tutorial/${tutorial.name}") })
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
          )
        }

        composable(
          "otherPlayerStats?playerId={playerId}",
          arguments = listOf(navArgument("playerId") { type = NavType.StringType })
        ) { backStackEntry ->
          StatsScreen()
        }

        composable("stats") {
          StatsScreen()
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
              navController.popBackStack()
            }
          )
        }
      }
    }
  }
}

@Composable
private fun BottomNavigationBar(navController: NavController, isLoggedIn: Boolean) {
  val items = listOf(
    BottomNavItem("myGames", "Play", ImageVector.vectorResource(R.drawable.ic_board_filled)),
    BottomNavItem("learn", "Learn", ImageVector.vectorResource(R.drawable.ic_learn)),
    BottomNavItem(
      "stats",
      "Stats",
      ImageVector.vectorResource(R.drawable.ic_diagram),
      enabled = isLoggedIn
    ),
    BottomNavItem(
      "settings", "Settings", ImageVector.vectorResource(R.drawable.ic_settings_filled),
    ),
  )

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 16.dp,
    ),
    border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  ) {
    NavigationBar {
      items.forEach { item ->
        NavigationBarItem(
          enabled = item.enabled,
          selected = currentRoute == item.route,
          onClick = {
            if (currentRoute != item.route) {
              navController.navigate(item.route) {
                popUpTo("myGames") { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            }
          },
          icon = { Icon(item.icon, contentDescription = item.label) },
          label = { Text(item.label) },
        )
      }
    }
  }
}

data class BottomNavItem(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val enabled: Boolean = true
)
