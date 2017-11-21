package io.zenandroid.onlinego.estimator

/**
 * Created by alex on 21/11/2017.
 */

class Estimator {

    external fun estimate(w: Int, h: Int, board: IntArray, playerToMove: Int, trials: Int, tolerance: Float): IntArray

    companion object {

        init {
            println("loading library")
            System.loadLibrary("estimator")
        }
    }

    //        val data = this [
    //                0, 0, 0, 0, 0, 0, 0, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0, 0, 0,
    //                0, 0, 1, 0, 0, 0,-1, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0, 0, 0,
    //                0, 0, 0, 1, 0, 0, 0, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0, 0, 0,
    //                0, 0, 1, 0,-1, 0,-1, 0, 0,
    //                0, 0, 0, 1,-1, 0, 0, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0, 0, 0
    //                ]
    //        val data = this [
    //                0, 0, 0, 0, 0, 0, 0, 0, 0,
    //                0, 0, 0, 0,-1, 0, 0, 0, 0,
    //                0, 0, 1, 0, 0, 0,-1, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0,-1, 0,
    //                0, 0, 0, 1, 0, 0, 0, 0, 0,
    //                0, 0, 0, 0, 0, 0, 0,-1, 0,
    //                0, 0, 1, 0,-1, 0,-1, 0, 0,
    //                0, 0, 0, 1,-1, 0, 0, 1, 0,
    //                0, 0, 0, 1,-1, 0, 0, 0, 0
    //                ]
    //        Estimator().estimate(9, 9, data, 1, 1000, 0.3f)
    //                .map { if(it == -1) 2 else it }
    //                .forEachIndexed { index, i ->
    //                    run {
    //                        print("$i ")
    //                        if (index % 9 == 8) println()
    //                    }
    //                }
    //        finish()

    //    operator fun get(vararg array: Int) = array
}
