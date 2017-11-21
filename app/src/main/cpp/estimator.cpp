/* vim: set tabstop=4 expandtab */
#include "estimator.hpp"
#ifndef EMSCRIPTEN
#  include <string.h>
#  include <stdlib.h>
#  include <stdio.h>
#include <jni.h>

#endif
#ifdef DEBUG
#  include <stdio.h>
#  include <ctype.h>
#  include <stdlib.h>
#  include <iostream>
#  include <boost/algorithm/string.hpp>
#endif

#define MAX(a,b) ((a) < (b) ? (b) : (a))
#define MIN(a,b) ((a) < (b) ? (a) : (b))

namespace score_estimator {

    extern "C"
    JNIEXPORT jintArray JNICALL
    Java_io_zenandroid_onlinego_estimator_Estimator_estimate(JNIEnv *env, jobject instance, jint width,
                                                             jint height, jintArray inBoard,
                                                             jint player_to_move, jint trials,
                                                             jfloat tolerance) {
        jint *data = env->GetIntArrayElements(inBoard, NULL);
        jint output[width*height];

        Goban g;
        g.width = width;
        g.height = height;
        for (int i=0, y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                    g.board[y][x] = data[i++];
            }
        }

        Goban est = g.estimate((Color)player_to_move, trials, tolerance);
        for (int i=0 ,y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                    output[i++] = est.board[y][x];
            }
        }

        env->ReleaseIntArrayElements(inBoard, data, JNI_ABORT);
        jintArray ret = env->NewIntArray(width*height);
        env->SetIntArrayRegion (ret, 0, width*height, output);
        return ret;
    }

    static Goban visited;
static int   last_visited_counter = 1;
#ifdef DEBUG
static const char board_letters[] = "abcdefghjklmnopqrstuvwxyz";
#endif


Goban::Goban() {
    width = 19;
    height = 19;
    memset(board, 0, sizeof(board));
    init();
}


Goban::Goban(const Goban &other) {
    width = other.width;
    height = other.height;
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            board[y][x] = other.board[y][x];
        }
    }
    init();
}


void Goban::init() {
    do_ko_check = 0;;
    possible_ko = Point(-1,-1);
}


Goban Goban::estimate(Color player_to_move, int trials, float tolerance) {
    Goban ret(*this);
    int   track[MAX_HEIGHT][MAX_WIDTH];

    do_ko_check = 0;;
    possible_ko = Point(-1,-1);
    memset(track, 0, sizeof(track));

    for (int i=0; i < trials; ++i) {
        /* Play out a random game */
        Goban t(*this);
        t.play_out_position(player_to_move);

        /* fill in territory */
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                Point p(x,y);
                if (t[p] == 0) {
                    if (t.is_territory(p, BLACK)) {
                        t.fill_territory(p, BLACK);
                    }
                    if (t.is_territory(p, WHITE)) {
                        t.fill_territory(p, WHITE);
                    }
                }
            }
        }

        /* track how many times each spot was white or black */
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                track[y][x] += t.board[y][x];
            }
        }
    }


    /* For each stone group, find the maximal track counter and set
     * all stones in that group to that level */
    Goban visited;
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (!visited[p]) {
                synchronize_tracking_counters(track, visited, p);
            }
        }
    }


    /* Create a result board based off of how many times each spot
     * was which color. */
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            /* If we're pretty confident we know who the spot belongs to, mark it */
            if (track[y][x] > trials*tolerance) {
                ret.board[y][x] = 1;
            } else if (track[y][x] < trials*-tolerance) {
                ret.board[y][x] = -1;
            /* if that fails, it's probably just dame */
            } else {
                ret.board[y][x] = 0;
            }
        }
    }


    /* TODO: Foreach hole, if it can only reach one color, color it that */
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (ret[p] == 0) {
                if (ret.is_territory(p, BLACK)) {
                    ret.fill_territory(p, BLACK);
                }
                if (ret.is_territory(p, WHITE)) {
                    ret.fill_territory(p, WHITE);
                }
            }
        }
    }


    return ret;
}


void Goban::synchronize_tracking_counters(int track[MAX_HEIGHT][MAX_WIDTH], Goban &visited, Point &p) {
    Vec         tocheck;
    NeighborVec neighbors;
    int         my_color        = (*this)[p];
    int         max_track_value = track[p.y][p.x];
    Vec         to_synchronize;

    tocheck.push(p);
    visited[p] = true;

    if (my_color == 0) {
        return;
    }

    while (tocheck.size) {
        Point p = tocheck.remove(0);
        to_synchronize.push(p);
        max_track_value = max_track_value < 0 ?
                            MIN(track[p.y][p.x], max_track_value) :
                            MAX(track[p.y][p.x], max_track_value);
        get_neighbors(p, neighbors);
        for (int i=0; i < neighbors.size; ++i) {
            Point neighbor = neighbors[i];
            if ((*this)[neighbor] == my_color) {
                if (visited[neighbor]) continue;
                visited[neighbor] = true;
                tocheck.push(neighbor);
            }
        }
    }

    for (int i=0; i < to_synchronize.size; ++i) {
        Point p = to_synchronize[i];
        track[p.y][p.x] = max_track_value;
    }
}


bool Goban::is_territory(Point pt, Color player) {
    Vec         tocheck;
    NeighborVec neighbors;
    int         visited_counter = ++last_visited_counter;

    tocheck.push(pt);
    visited[pt] = visited_counter;

    while (tocheck.size) {
        Point p = tocheck.remove(0);
        if ((*this)[p] == 0) {
            get_neighbors(p, neighbors);
            for (int i=0; i < neighbors.size; ++i) {
                Point neighbor = neighbors[i];
                if (visited[neighbor] == visited_counter) continue;
                visited[neighbor] = visited_counter;
                tocheck.push(neighbor);
            }
        } else {
            if ((*this)[p] != player) {
                return false;
            }
        }
    }

    return true;;
}


void Goban::fill_territory(Point pt, Color player) {
    Vec         tocheck;
    NeighborVec neighbors;
    int         visited_counter = ++last_visited_counter;

    tocheck.push(pt);
    visited[pt] = visited_counter;

    while (tocheck.size) {
        Point p = tocheck.remove(0);
        if ((*this)[p] == 0) {
            (*this)[p] = player;
            get_neighbors(p, neighbors);
            for (int i=0; i < neighbors.size; ++i) {
                Point neighbor = neighbors[i];
                if (visited[neighbor] == visited_counter) continue;
                visited[neighbor] = visited_counter;
                tocheck.push(neighbor);
            }
        }
    }
}


void Goban::play_out_position(Color player_to_move) {
    Vec possible_moves;
    Vec illegal_moves;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            if (board[y][x] == 0) {
                possible_moves.push(Point(x,y));
            }
        }
    }



    int sanity = 1000;
    while (possible_moves.size > 0 && --sanity > 0) {
        int move_idx = rand() % possible_moves.size;
        Point mv(possible_moves[move_idx]);

        if (is_eye(mv, player_to_move)) {
            illegal_moves.push(possible_moves.remove(move_idx));
            continue;
        }

        int result = place_and_remove(mv, player_to_move, possible_moves);
        if (result == OK) {
            possible_moves.remove(move_idx);
            player_to_move = (Color)-player_to_move;
            for (int i=0; i < illegal_moves.size; ++i) {
                possible_moves.push(illegal_moves[i]);
            }
            illegal_moves.size = 0;
            continue;
        }
        else if (result == ILLEGAL) {
            illegal_moves.push(possible_moves.remove(move_idx));

            continue;
        }
    }
}


void Goban::get_neighbors(const Point &pt, NeighborVec &output) {
    output.size = 0;
    if (pt.x > 0)        output.push(Point(pt.x-1, pt.y));
    if (pt.x+1 < width)  output.push(Point(pt.x+1, pt.y));
    if (pt.y > 0)        output.push(Point(pt.x, pt.y-1));
    if (pt.y+1 < height) output.push(Point(pt.x, pt.y+1));
}


Result Goban::place_and_remove(Point move, Color player, Vec &possible_moves) {
    if (do_ko_check) {
        if (move == possible_ko) {
            return ILLEGAL;
        }
    }

    bool        reset_ko_check = true;
    bool        removed        = false;
    NeighborVec neighbors;

    this->get_neighbors(move, neighbors);

    (*this)[move] = player;
    ++last_visited_counter;
    for (int i=0; i < neighbors.size; ++i) {
        if ((*this)[neighbors[i]] == -player) {
            if (
                /* it's common that a previous has_liberties covers what we're
                 * about to test, so don't double test */
                visited[neighbors[i]] != last_visited_counter
                && !has_liberties(neighbors[i])
            ) {
                if (remove_group(neighbors[i], possible_moves) == 1) {
                    reset_ko_check = false;
                    do_ko_check = 1;
                    possible_ko = neighbors[i];
                }
                removed = true;
            }
        }
    }
    if (!removed) {
        if (!has_liberties(move)) {
            (*this)[move] = 0;
            return ILLEGAL;
        }
    }

    if (reset_ko_check) {
        do_ko_check = false;
    }
    return OK;
}


bool Goban::has_liberties(const Point &pt) {
    Vec tocheck;
    int w_1                = width-1;
    int h_1                = height-1;
    int my_color           = (*this)[pt];
    int my_visited_counter = ++last_visited_counter;

    tocheck.push(pt);
    visited[pt] = my_visited_counter;

    while (tocheck.size) {
        Point p = tocheck.remove(tocheck.size-1);

        if (p.x > 0) {
            Point &neighbor = tocheck[tocheck.size];
            neighbor.x = p.x-1;
            neighbor.y = p.y;
            int c = board[neighbor.y][neighbor.x];
            if (c == 0) {
                return true;
            }
            if (c == my_color && visited.board[neighbor.y][neighbor.x] != my_visited_counter) {
                visited.board[neighbor.y][neighbor.x] = my_visited_counter;
                ++tocheck.size;
            }
        }
        if (p.x < w_1) {
            Point &neighbor = tocheck[tocheck.size];
            neighbor.x = p.x+1;
            neighbor.y = p.y;
            int c = board[neighbor.y][neighbor.x];
            if (c == 0) {
                return true;
            }
            if (c == my_color && visited.board[neighbor.y][neighbor.x] != my_visited_counter) {
                visited.board[neighbor.y][neighbor.x] = my_visited_counter;
                ++tocheck.size;
            }
        }
        if (p.y > 0) {
            Point &neighbor = tocheck[tocheck.size];
            neighbor.x = p.x;
            neighbor.y = p.y-1;
            int c = board[neighbor.y][neighbor.x];
            if (c == 0) {
                return true;
            }
            if (c == my_color && visited.board[neighbor.y][neighbor.x] != my_visited_counter) {
                visited.board[neighbor.y][neighbor.x] = my_visited_counter;
                ++tocheck.size;
            }
        }
        if (p.y < h_1) {
            Point &neighbor = tocheck[tocheck.size];
            neighbor.x = p.x;
            neighbor.y = p.y+1;
            int c = board[neighbor.y][neighbor.x];
            if (c == 0) {
                return true;
            }
            if (c == my_color && visited.board[neighbor.y][neighbor.x] != my_visited_counter) {
                visited.board[neighbor.y][neighbor.x] = my_visited_counter;
                ++tocheck.size;
            }
        }
    }


    return false;
}


int  Goban::remove_group(Point move, Vec &possible_moves) {
    Goban       visited;
    Vec         tocheck;
    NeighborVec neighbors;
    int         n_removed = 0;
    int         my_color  = (*this)[move];

    tocheck.push(move);
    visited[move] = true;

    while (tocheck.size) {
        Point p = tocheck.remove(0);

        (*this)[p] = 0;
        possible_moves.push(p);
        n_removed++;

        get_neighbors(p, neighbors);

        for (int i=0; i < neighbors.size; ++i) {
            Point neighbor = neighbors[i];
            if (visited[neighbor]) continue;
            visited[neighbor] = true;

            int c = (*this)[neighbor];
            if (c == my_color) {
                tocheck.push(neighbor);
            }
        }
    }

    return n_removed;;
}


bool Goban::is_eye(Point pt, Color player) {
    if ((pt.x == 0        || board[pt.y][pt.x-1] == player) &&
        (pt.x == width-1  || board[pt.y][pt.x+1] == player) &&
        (pt.y == 0        || board[pt.y-1][pt.x] == player) &&
        (pt.y == height-1 || board[pt.y+1][pt.x] == player))
    {
        return true;
    }
    return false;
}


int  Goban::score() {
    int ret = 0;
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            ret += board[y][x];
        }
    }
    return ret;
}


void Goban::setSize(int width, int height) {
    this->width = width;
    this->height = height;
}


void Goban::clearBoard() {
    memset(board, 0, sizeof(board));
}




#ifdef DEBUG
void Goban::print() {
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            printf("%c ", board[y][x] == -1 ? '_' : (board[y][x] == 1 ? 'o' : ' '));
        }
        printf("\n");
    }
}


void Goban::showBoard() {
    printf("    ");
    for (int x=0; x < width; ++x) {
        printf("%c ", board_letters[x]);
    }
    printf("  \n");

    printf("  ");
    for (int x=0; x <= width; ++x) {
        printf("%c", x == 0 ? '+' : '-');
        printf("%c", '-');
    }
    printf("+  \n");

    for (int y=0; y < height; ++y) {
        printf("%2d|", 19-y);
        for (int x=0; x < width; ++x) {
            printf(" %c", board[y][x] == 0 ? '.' : (board[y][x] == 1 ? 'X' : 'O'));
        }
        printf(" |%-2d\n", 19-y);
    }

    printf("  ");
    for (int x=0; x <= width; ++x) {
        printf("%c", x == 0 ? '+' : '-');
        printf("%c", '-');
    }
    printf("+  \n");

    printf("    ");
    for (int x=0; x < width; ++x) {
        printf("%c ", board_letters[x]);
    }
    printf("  \n");
}


Point Goban::pointFromStr(const char *str) {
    int x=-1, y=-1;
    if (tolower(str[0]) == 'p' && tolower(str[1]) == 'a')  { x=y=-1; return Point(x,y); } /* pass */
    if (tolower(str[0]) == 'r' && tolower(str[1]) == 'e')  { x=y=-100; return Point(x,y); } /* resign */

    x = index(board_letters, tolower(str[0])) - board_letters;
    y = height-atoi(str+1);

    if (x < 0 || y < 0 || x >= width || y >= height) {
        throw "Invalid vertex";
    }

    return Point(x,y);
}


ostream& operator<<(ostream &o, const Point &pt) {
    if (pt.x >= 0) {
        o << board_letters[pt.x] << 19 - pt.y;
    }
    if (pt.x == -1) {
        o << "pass";
    }
    if (pt.x == -100) {
        o << "resign";
    }
    return o;
}
#endif

} /* namespace baduk */
