package io.zenandroid.onlinego.gamelogic

internal expect object NativeEstimator {
  fun estimate(
    w: Int,
    h: Int,
    board: IntArray,
    playerToMove: Int,
    trials: Int,
    tolerance: Float
  ): IntArray
}
