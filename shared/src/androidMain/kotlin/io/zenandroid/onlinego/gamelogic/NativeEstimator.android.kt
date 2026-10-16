package io.zenandroid.onlinego.gamelogic

import co.touchlab.kermit.Logger

internal actual object NativeEstimator {

  init {
    try {
      System.loadLibrary("estimator")
    } catch (_: UnsatisfiedLinkError) {
      Logger.e("Error loading estimator", tag = "libestimator")
    }
  }

  actual external fun estimate(
    w: Int,
    h: Int,
    board: IntArray,
    playerToMove: Int,
    trials: Int,
    tolerance: Float
  ): IntArray
}
