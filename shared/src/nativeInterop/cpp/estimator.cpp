#define EMSCRIPTEN
#define USE_THREADS 1

#include "estimator.h"
#include "Goban.h"
#include "Goban.cpp"

extern "C" void
estimator_estimate(int width, int height, int *board, int player_to_move, int trials,
                   float tolerance) {
    Goban g(width, height);
    for (int i = 0, y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            g.board[y][x] = board[i++];
        }
    }

    Grid est = g.estimate((Color) player_to_move, trials, tolerance, false);
    for (int i = 0, y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            board[i++] = est[y][x];
        }
    }
}
