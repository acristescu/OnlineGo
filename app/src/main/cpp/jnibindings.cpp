//
// Created by alex on 11/10/2018.
//
#define EMSCRIPTEN
#define USE_THREADS 1

#include <jni.h>
#include "Goban.h"
#include "Goban.cpp"

extern "C"
JNIEXPORT jintArray JNICALL
Java_io_zenandroid_onlinego_gamelogic_RulesManager_estimate(JNIEnv *env, jobject instance, jint width,
                                                            jint height, jintArray inBoard,
                                                            jint player_to_move, jint trials,
                                                            jfloat tolerance) {
    jint *data = env->GetIntArrayElements(inBoard, NULL);
    jint output[width*height];

    Goban g(width, height);
    for (int i=0, y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            g.board[y][x] = data[i++];
        }
    }

    Grid est = g.estimate((Color)player_to_move, trials, tolerance, false);
    for (int i=0 ,y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            output[i++] = est[y][x];
        }
    }

    env->ReleaseIntArrayElements(inBoard, data, JNI_ABORT);
    jintArray ret = env->NewIntArray(width*height);
    env->SetIntArrayRegion (ret, 0, width*height, output);
    return ret;
}