package io.zenandroid.onlinego.gamelogic

import io.zenandroid.onlinego.gamelogic.cinterop.estimator_estimate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

internal actual object NativeEstimator {

  @OptIn(ExperimentalForeignApi::class)
  actual fun estimate(
    w: Int,
    h: Int,
    board: IntArray,
    playerToMove: Int,
    trials: Int,
    tolerance: Float
  ): IntArray {
    val result = board.copyOf()
    result.usePinned { estimator_estimate(w, h, it.addressOf(0), playerToMove, trials, tolerance) }
    return result
  }
}
