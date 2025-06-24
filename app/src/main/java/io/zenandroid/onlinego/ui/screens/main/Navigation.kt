package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.ui.screens.face2face.FaceToFaceScreen
import io.zenandroid.onlinego.ui.screens.game.GameScreen
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerScreen
import io.zenandroid.onlinego.ui.screens.learn.LearnScreen
import io.zenandroid.onlinego.ui.screens.localai.AiGameScreen
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesScreen
import io.zenandroid.onlinego.ui.screens.puzzle.directory.PuzzleDirectoryScreen
import io.zenandroid.onlinego.ui.screens.puzzle.tsumego.TsumegoScreen
import io.zenandroid.onlinego.ui.screens.settings.SettingsScreen
import io.zenandroid.onlinego.ui.screens.stats.StatsScreen
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialScreen
import io.zenandroid.onlinego.ui.theme.LocalBoardTheme
import io.zenandroid.onlinego.ui.theme.LocalShowCoordinates
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import org.koin.compose.koinInject


@Composable
fun OnlineGoApp(isLoggedIn: Boolean) {
  val navController = rememberNavController()

  val startDestination = if (isLoggedIn) "myGames" else "onboarding"

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route
  val settingsRepository: SettingsRepository = koinInject()

  val showBottomBar = currentDestination in listOf("myGames", "learn", "stats", "settings")
  val m3Destinations = listOf("myGames", "learn", "stats", "settings", "puzzleDirectory")

  // TODO: make the settings reactive flows
  CompositionLocalProvider(
    LocalBoardTheme provides remember { settingsRepository.boardTheme },
    LocalShowCoordinates provides remember { settingsRepository.showCoordinates },
  ) {
    OnlineGoTheme(m3 = m3Destinations.contains(currentDestination)) {
      Scaffold(bottomBar = {
        if (showBottomBar) {
          BottomNavigationBar(navController)
        }
      }) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = startDestination,
          modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        ) {
          composable("myGames") {
            MyGamesScreen(
              onNavigateToGame = { navController.navigate("game/${it.id}/${it.width}/${it.height}") },
              onNavigateToAIGame = { navController.navigate("aiGame") },
              onNavigateToFaceToFace = { navController.navigate("faceToFace") },
            )
          }

          composable("aiGame") {
            AiGameScreen(
              onNavigateBack = { navController.popBackStack() },
            )
          }

          composable("faceToFace") {
            FaceToFaceScreen(
              onNavigateBack = { navController.popBackStack() },
            )
          }

          composable(
            "game/{gameId}/{gameWidth}/{gameHeight}",
            arguments = listOf(
              navArgument("gameId") { type = NavType.LongType },
              navArgument("gameWidth") { type = NavType.IntType },
              navArgument("gameHeight") { type = NavType.IntType })
          ) { backStackEntry ->
            GameScreen(
              onNavigateBack = { navController.popBackStack() },
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
              onNavigateBack = { navController.popBackStack() })
          }

          composable("josekiExplorer") {
            JosekiExplorerScreen(
              onNavigateBack = { navController.popBackStack() })
          }

          composable("settings") {
            SettingsScreen(
              onNavigateToLogin = {
                navController.navigate(
                  "onboarding",
                  navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo("myGames", inclusive = true).build()
                )
              },
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
              onNavigateBack = { navController.popBackStack() },
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
              onNavigateBack = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BottomNavigationBar(navController: NavController) {
  val items = listOf(
    BottomNavItem("myGames", "Play", ImageVector.vectorResource(R.drawable.ic_board_filled)),
    BottomNavItem("learn", "Learn", ImageVector.vectorResource(R.drawable.ic_learn)),
    BottomNavItem("stats", "Stats", ImageVector.vectorResource(R.drawable.ic_diagram)),
    BottomNavItem(
      "settings", "Settings", ImageVector.vectorResource(R.drawable.ic_settings_filled),
    ),
  )

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  NavigationBar {
    items.forEach { item ->
      NavigationBarItem(
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

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
