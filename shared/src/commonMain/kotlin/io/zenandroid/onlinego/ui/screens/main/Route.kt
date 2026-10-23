package io.zenandroid.onlinego.ui.screens.main

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
  @Serializable
  @SerialName("myGames")
  data object MyGames : Route
  @Serializable
  @SerialName("aiGame")
  data object AiGame : Route
  @Serializable
  @SerialName("faceToFace")
  data object FaceToFace : Route
  @Serializable
  @SerialName("game")
  data class Game(val gameId: Long, val gameWidth: Int, val gameHeight: Int) : Route
  @Serializable
  @SerialName("learn")
  data object Learn : Route
  @Serializable
  @SerialName("tutorial")
  data class Tutorial(val tutorialName: String) : Route
  @Serializable
  @SerialName("josekiExplorer")
  data object JosekiExplorer : Route
  @Serializable
  @SerialName("settings")
  data object Settings : Route
  @Serializable
  @SerialName("socketDebug")
  data object SocketDebug : Route
  @Serializable
  @SerialName("otherPlayerStats")
  data class OtherPlayerStats(val playerId: Long) : Route
  @Serializable
  @SerialName("stats")
  data object Stats : Route
  @Serializable
  @SerialName("puzzleDirectory")
  data object PuzzleDirectory : Route
  @Serializable
  @SerialName("tsumego")
  data class Tsumego(val collectionId: Long, val puzzleId: Long) : Route
  @Serializable
  @SerialName("supporter")
  data object Supporter : Route
  @Serializable
  @SerialName("onboarding")
  data class Onboarding(val initialPage: String? = null) : Route
}
