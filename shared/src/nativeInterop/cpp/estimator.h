#pragma once

#ifdef __cplusplus
extern "C" {
#endif

void estimator_estimate(int width, int height, int *board, int player_to_move, int trials,
                        float tolerance);

#ifdef __cplusplus
}
#endif
