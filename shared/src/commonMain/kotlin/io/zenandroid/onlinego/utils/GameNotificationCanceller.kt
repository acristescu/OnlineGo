package io.zenandroid.onlinego.utils

fun interface GameNotificationCanceller {
  fun cancelGameNotification(gameId: Long)
}
