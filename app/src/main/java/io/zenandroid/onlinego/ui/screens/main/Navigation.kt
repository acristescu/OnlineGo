package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesScreen
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme


@Composable
fun OnlineGoApp(isLoggedIn: Boolean) {
  val navController = rememberNavController()

  val startDestination = if (isLoggedIn) "myGames" else "onboarding"

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route

  val showBottomBar = currentDestination in listOf("myGames", "learn", "stats", "settings")
  val m3Destinations = listOf("myGames")
  OnlineGoTheme(m3 = m3Destinations.contains(currentDestination)) {
    Scaffold(bottomBar = {
      if (showBottomBar) {
        BottomNavigationBar(navController)
      }
    }) { innerPadding ->
      NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(innerPadding)
      ) {
        composable("myGames") {
          MyGamesScreen(
            onNavigateToGame = { navController.navigate("game") },
            onNavigateToAIGame = { navController.navigate("aiGame") },
          )
        }

        composable("aiGame") {
          // AI Game Screen
          Text(text = "AI Game Screen")
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
    BottomNavItem("settings", "Settings", ImageVector.vectorResource(R.drawable.ic_settings_filled),
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
