#pragma once

#define MAX_WIDTH 25
#define MAX_HEIGHT 25
#define MAX_VEC_SIZE (MAX_WIDTH*MAX_HEIGHT)

#define MAX(a,b) ((a) < (b) ? (b) : (a))
#define MIN(a,b) ((a) < (b) ? (a) : (b))

#ifdef USE_THREADS
#  define THREAD_LOCAL thread_local
#else
#  define THREAD_LOCAL
#endif

static THREAD_LOCAL int default_grid_width = -1000000;
static THREAD_LOCAL int default_grid_height = -1000000;

#ifdef DEBUG
static const char board_letters[] = "abcdefghjklmnopqrstuvwxyz";
#endif