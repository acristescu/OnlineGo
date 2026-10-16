#include <jni.h>
#include "estimator.h"

extern "C"
JNIEXPORT jintArray JNICALL
Java_io_zenandroid_onlinego_gamelogic_NativeEstimator_estimate(JNIEnv *env, jobject instance,
                                                               jint width,
                                                               jint height, jintArray inBoard,
                                                               jint player_to_move, jint trials,
                                                               jfloat tolerance) {
    jint size = width * height;
    jint board[size];
    env->GetIntArrayRegion(inBoard, 0, size, board);

    estimator_estimate(width, height, board, player_to_move, trials, tolerance);

    jintArray ret = env->NewIntArray(size);
    env->SetIntArrayRegion(ret, 0, size, board);
    return ret;
}
