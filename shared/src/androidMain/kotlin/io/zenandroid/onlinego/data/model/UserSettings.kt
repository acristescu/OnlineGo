package io.zenandroid.onlinego.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserSettings(
  val theme: AppTheme = AppTheme.DEFAULT,
  val boardTheme: BoardTheme = BoardTheme.WOOD,
  val showRanks: Boolean = true,
  val showCoordinates: Boolean = false,
  val soundEnabled: Boolean = true,
  val graphByGames: Boolean = false,
)
