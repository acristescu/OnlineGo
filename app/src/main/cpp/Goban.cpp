/* vim: set tabstop=4 expandtab */
#include "Goban.h"
#include "log.h"
#include <set>

#  include <stdlib.h>

#ifndef EMSCRIPTEN
#  include <string.h>
#  include <stdlib.h>
#  include <stdio.h>
#endif

#ifdef DEBUG
#  include <stdio.h>
#  include <ctype.h>
#  include <stdlib.h>
#  include <iostream>

#endif

using namespace std;

Goban::Goban(int width, int height)
        : width(width)
        , height(height)
        , board(width, height)
        , global_visited(width, height)
        , last_visited_counter(1)
#ifdef USE_THREADS
, rand(::rand())
#endif
{
    default_grid_width = width;
    default_grid_height = height;
}

Goban::Goban(const Goban &other) {
    this->width = other.width;
    this->height = other.height;
    this->board = other.board;
    this->do_ko_check = other.do_ko_check;
    this->possible_ko = other.possible_ko;

    this->global_visited = other.global_visited;
    this->last_visited_counter = other.last_visited_counter;

#ifdef USE_THREADS
    rand = std::mt19937(::rand());
#endif
}

void Goban::setBoardSize(int width, int height) {
    this->width = width;
    this->height = height;
    board.width = width;
    board.height = height;
    global_visited.width = width;
    global_visited.height = height;

    board.clear();
    global_visited.clear();

    default_grid_width = width;
    default_grid_height = height;
}
Grid Goban::estimate(Color player_to_move, int num_iterations, float tolerance, bool debug) const {
    Goban t(*this);
    return t._estimate(player_to_move, num_iterations, tolerance, debug);
}

Grid Goban::_estimate(Color player_to_move, int num_iterations, float tolerance, bool debug) {
    default_grid_width = width;
    default_grid_height = height;


#ifndef EMSCRIPTEN
    if (debug) {
        Vec false_eyes = getFalseEyes();
        if (false_eyes.size) {
            NOTE << "False eyes: " << false_eyes << endl;
        }
    }
#endif

    fillFalseEyes();

    /* Look for seki, or similar situations */
    int seki_pass_iterations = num_iterations;
    Grid seki_pass = rollout(seki_pass_iterations, player_to_move, false);
    //Grid seki = scanForSeki(num_iterations, tolerance, seki_pass);
    Grid seki = scanForSeki(num_iterations, 0.2, seki_pass);

#ifndef EMSCRIPTEN
    if (debug) {
        printf("\nSeki pass:\n");
        seki_pass.printInts(" %6d", "      ");
        printf("\nSeki:\n");
        seki.printInts();
    }
#endif

    Grid horseshoe_bias;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (board[p] == 0 && (is_safe_horseshoe(p, BLACK) || is_safe_horseshoe(p, WHITE))) {
                Vec neighbors ;
                board.getNeighbors(p, neighbors);
                //if (debug) { NOTE << p << " was horseshoe" << endl; }
                for (int i=0; i< neighbors.size; ++i) {
                    Vec gr = board.group(neighbors[i]);
                    horseshoe_bias.set(gr, 1);
                }
            }
        }
    }

    horseshoe_bias *= board;
    horseshoe_bias *= (num_iterations * (tolerance / 2));


    Grid ret;
    Grid pass1;
    float tolerance_scale = 1;

    tolerance *= tolerance_scale;
    //Grid bias = computeBias(num_iterations, tolerance);

    Grid bias = computeBias(num_iterations, tolerance);
    Grid territory_map = computeTerritory();
    Grid group_map = computeGroupMap();
    Grid liberty_map = computeLiberties(group_map);
    Grid strong_life = computeStrongLife(group_map, territory_map, liberty_map);

    //bias += (seki * board) * (int)(num_iterations * tolerance) * 2;

    Grid liberty_bias = biasLibertyMap(num_iterations, tolerance, liberty_map);
    //bias += liberty_bias;

    Grid likely_dead = biasLikelyDead(num_iterations, tolerance, liberty_map);
    //bias += likely_dead;

    bias += horseshoe_bias;

    tolerance /= tolerance_scale ;

    //bias.clear();
    //territory_map.clear();
    //liberty_map.clear();
    //strong_life.clear();


#ifndef EMSCRIPTEN
    if (debug) {
        printf("\nHorseshoe bias:\n");
        horseshoe_bias.printInts();

        printf("\nLikely dead:\n");
        likely_dead.printInts();

        printf("\nLiberty bias:\n");
        liberty_bias.printInts();

        printf("\nBias map:\n");
        bias.printInts(" %5d", "     ");
        printf("\nTerritory map:\n");
        territory_map.printInts();
        printf("\nGroup map:\n");
        group_map.printInts();
        printf("\nLiberty map:\n");
        liberty_map.printInts();
        printf("\nLife map:\n");
        strong_life.printInts();
    }
#endif


    int pass1_iterations = num_iterations;
    //Grid pass1 = rollout(pass1_iterations, player_to_move, strong_life, bias);
    //pass1 = rollout(pass1_iterations, player_to_move, strong_life, bias);
    pass1 = rollout(pass1_iterations, player_to_move, true, strong_life, bias, seki);
    Vec dead = getDead(pass1_iterations, tolerance, pass1);

#ifndef EMSCRIPTEN
    if (debug) {
        printf("\nBias :\n");
        bias.printInts(" %6d", "      ");
        printf("\nPass1 :\n");
        pass1.printInts(" %6d", "      ");
    }
#endif

    /* remove obviously dead stones from first pass */
    //goban_pass2.board.set(dead, 0);
    //board.set(dead, 0);

#ifndef EMSCRIPTEN
    Grid removed;
    removed.set(dead, 1);
    if (debug) {
        printf("\nRemoved from pass1:\n");
        removed.printInts();
    }
#endif


    /* Create a result board based off of how many times each spot was which color. */
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            /* If we're pretty confident we know who the spot belongs to, mark it */
            if (pass1[y][x] > num_iterations * tolerance) {
                ret[y][x] = 1;
            } else if (pass1[y][x] < num_iterations * -tolerance) {
                ret[y][x] = -1;
                /* if that fails, it's probably just dame */
            } else {
                ret[y][x] = 0;
            }
        }
    }


    /* TODO: Foreach hole, if it can only reach one color, color it that */
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Vec group, neighbors;
            Point p(x,y);
            if (ret[p] == 0) {
                board.groupAndNeighbors(p, group, neighbors);
                if (ret.allNotEqualTo(neighbors, WHITE)) {
                    ret.set(group, BLACK);
                } else if (ret.allNotEqualTo(neighbors, BLACK)) {
                    ret.set(group, WHITE);
                }
            }
        }
    }

    return ret;
}

Grid Goban::biasLibertyMap(int num_iterations, float tolerance, const Grid &liberty_map) const {
    Grid ret(width, height);

    /*
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (board[p] == 0 && liberty_map[p] > 6) {
                ret[p] = BLACK * (num_iterations * tolerance / 2);
            }
            if (board[p] == 0 && liberty_map[p] < -6) {
                ret[p] = WHITE * (num_iterations * tolerance / 2);
            }
        }
    }
    */

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            Vec neighbors;
            liberty_map.getNeighbors(p, neighbors);

            int sum = liberty_map[p] + liberty_map.sum(neighbors);
            ret[p] = (sum * (num_iterations * tolerance)) / 100;
            //ret[p] = 0;
            /*
            if (board[p] == 0 && liberty_map[p] > 6) {
                ret[p] = BLACK * (num_iterations * tolerance / 2);
            }
            if (board[p] == 0 && liberty_map[p] < -6) {
                ret[p] = WHITE * (num_iterations * tolerance / 2);
            }
            */
        }
    }

    return ret;
}
Grid Goban::biasLikelyDead(int num_iterations, float tolerance, const Grid &liberty_map) const {
    Grid ret(width, height);
    Grid visited(width, height);

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (board[p] != 0) {
                if (visited[p]) {
                    continue;
                }
                Vec group = board.group(p);
                visited.set(group, 1);
                if (group.size == 1) {
                    Vec neighbors;
                    board.getNeighbors(p, neighbors);
                    int remove = 1;
                    for (int i=0; i < neighbors.size; ++i) {
                        if (!((liberty_map[neighbors[i]] > 6 && board[p] == WHITE) || (liberty_map[neighbors[i]] < 6 && board[p] == BLACK)))  {
                            remove = 0;
                        }
                    }
                    ret[p] = remove * -board[p] * (num_iterations * tolerance / 2);
                }
            }
        }
    }

    return ret;
}
Grid Goban::scanForSeki(int num_iterations, float tolerance, const Grid &rollout_pass) const {
    Grid seki;
    Grid visited;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            Vec group, neighbors;

            if (visited[p]) {
                continue;
            }

            board.groupAndNeighbors(p, group, neighbors);
            visited.set(group, 1);

            for (int color : { BLACK , WHITE }) {
                int other = - color;

                if (board[p] == color && rollout_pass.allLTE(group, num_iterations * tolerance) && rollout_pass.allGTE(group, 1)) {
                    Vec neighboring = board.match(neighbors, other);
                    int my_liberties = board.countEqual(neighbors, EMPTY);

                    /* If it is questionable that we are dead, and we are neighboring another group
                     * that is questionably dead - consider ourselves in seki if both groups have the
                     * same number of liberties. This is not a true seki detection and doesn't work
                     * in all possible cases, however it's pretty reasonable. */
                    if (rollout_pass.anyAbsLTE(neighboring, num_iterations * tolerance)) {
                        int in_seki = true;

                        for (int i=0; i <  neighboring.size; ++i) {

                            if (abs(rollout_pass[neighboring[i]]) < num_iterations * tolerance) {
                                Vec neighbor_group, neighbor_neighbors;
                                board.groupAndNeighbors(neighboring[i], neighbor_group, neighbor_neighbors);

                                int neighbor_liberties = board.countEqual(neighbor_neighbors, EMPTY);
                                if (neighbor_liberties != my_liberties) {
                                    in_seki = false;
                                }
                            }
                        }

                        if (in_seki) {
                            seki.set(group, 1);
                            Vec territory = board.match(neighbors, EMPTY);
                            seki.set(territory, 1);
                        }
                    }

                    /*
                    if (rollout_pass.anyAbsLTE(neighboring, num_iterations * tolerance)
                        && board.countEqual(neighbors, EMPTY) <= 2
                        //&& !rollout_pass.anyAbsGTE(neighboring, num_iterations * tolerance)
                        )
                    {
                        seki.set(group, 1);
                        Vec territory = board.match(neighbors, EMPTY);
                        seki.set(territory, 1);
                    }
                    */
                }
            }
        }
    }

    return seki;
}

Grid Goban::rollout(int num_iterations, Color player_to_move, bool pullup_life_based_on_neigboring_territory, const Grid &life_map, const Grid &bias, const Grid &seki) const {
    Grid ret = bias;

    for (int i=0; i < num_iterations; ++i) {
        /* Play out a random game */
        Goban t(*this);

        t.play_out_position(player_to_move, life_map, seki);

        //t.board.print();

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
                ret[y][x] += t.board[y][x];
            }
        }
    }


    /* For each stone group, find the maximal track counter and set
     * all stones in that group to that level */
    {
        Grid visited(width, height);

        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                Point p(x,y);

                if (!visited[p] && board[p]) {
                    Vec group, neighbors;
                    board.groupAndNeighbors(p, group, neighbors);
                    int minmax = ret.minmax(group);
                    visited.set(group, 1);


                    if (pullup_life_based_on_neigboring_territory) {
                        /* If we are adjacent to any territory which is a higher
                         * value than ourselves, set ourselves to that value */
                        if (minmax < 0) {
                            minmax = MIN(ret.min(neighbors), minmax);
                        }

                        if (minmax > 0) {
                            minmax = MAX(ret.max(neighbors), minmax);
                        }
                    }

                    ret.set(group, minmax);
                }
            }
        }
    }



    //return ret + bias;
    return ret;
}
Grid Goban::computeBias(int num_iterations, float tolerance) {
    Grid bias(width, height);
    /* Bias our scoring towards trusting the player's area is theirs */
    Goban t(*this);

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

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            bias[y][x] += t.board[y][x] * num_iterations * (tolerance/2);
        }
    }

    return bias;
}
Grid Goban::computeGroupMap() const{
    Grid ret(width, height);
    int cur_group = 1;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (ret[p] == 0) {
                board.traceGroup(p, ret, cur_group++);
            }
        }
    }

    return ret;
}
Grid Goban::computeTerritory() {
    Grid ret(width, height);

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (ret[p]) {
                continue;
            }

            Vec group, neighbors;
            if (board[p] == 0 && is_territory(p, BLACK)) {
                board.groupAndNeighbors(p, group, neighbors);
                ret.set(group, group.size * BLACK);
            }
            if (board[p] == 0 && is_territory(p, WHITE)) {
                board.groupAndNeighbors(p, group, neighbors);
                ret.set(group, group.size * WHITE);
            }
        }
    }

    return ret;
}
Grid Goban::computeLiberties(const Grid &group_map) const {
    Grid ret(width, height);
    Grid visited(width, height);

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (visited[p]) {
                continue;
            }

            Vec group;
            Vec neighbors;
            board.groupAndNeighbors(p, group, neighbors);
            visited.set(group, 1);


            int liberty_count = 0;
            if ((*this)[p] == 0) {
                /* sum of all ajacent black - all ajacent white */
                for (int i=0; i < neighbors.size; ++i) {
                    liberty_count += (*this)[neighbors[i]];
                }
            } else {
                /* liberties of stone group */
                for (int i=0; i < neighbors.size; ++i) {
                    if ((*this)[neighbors[i]] == 0) {
                        liberty_count += (*this)[p];
                    }
                }
            }

            ret.set(group, liberty_count);
        }
    }

    return ret;
}
Grid Goban::computeStrongLife(const Grid &groups, const Grid &territory, const Grid &liberties) const {
    Grid ret(width, height);
    Grid visited(width, height);

    Grid unified_territory_and_stones(width, height);
    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            unified_territory_and_stones[p] = board[p] == 0 ? (territory[p] <= -1 ? -1 : territory[p] >= 1 ? 1 : 0) : board[p];
        }
    }


    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (visited[p]) {
                continue;
            }

            Vec group;
            Vec neighbors;
            unified_territory_and_stones.groupAndNeighbors(p, group, neighbors);

            int num_eyes = 0;
            int num_territory = 0;
            for (int i=0; i < group.size; ++i) {
                if (visited[group[i]]) {
                    continue;
                }
                if (territory[group[i]]) {
                    Vec territory_group;
                    Vec territory_neighbors;
                    board.groupAndNeighbors(group[i], territory_group, territory_neighbors);
                    visited.set(territory_group, 1);
                    num_eyes += 1;
                    num_territory += territory_group.size;
                }
            }

            visited.set(group, 1);
            if (num_eyes >= 2 || num_territory >= 5) {
                ret.set(group, num_territory);
            }
        }
    }

    return ret;
}
Vec Goban::getDead(int num_iterations, float tolerance, const Grid &rollout_pass) const {
    Vec removed;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            /* If we're pretty confident we know who the spot belongs to, mark it */
            if (rollout_pass[p] > num_iterations * tolerance) {
                if (board[p] == -1) { /* pretty sure it's black, but it was white? removed. */
                    removed.push(p);
                }
            } else if (rollout_pass[y][x] < num_iterations * -tolerance) {
                if (board[p] == 1) { /* pretty sure it's white, but it was black? removed. */
                    removed.push(p);
                }
            } else {
                /* No idea who the spot belongs to, but it was a color? Probably removed. */
                if (board[p] != 0) {
                    removed.push(p);
                }
            }
        }
    }

    return removed;
}
Vec Goban::getFalseEyes() const {
    Vec false_eyes;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (at(p) == 0) {
                Vec neighbors;
                Vec corners;
                board.getNeighbors(p, neighbors);
                board.getCornerPoints(p, corners);

                if (corners.size == 1) {
                    continue;
                }

                if (board.allEqualTo(neighbors, BLACK) && board.countEqual(corners, WHITE) >= (corners.size>>1)) {
                    false_eyes.push(p);
                }
                else if (board.allEqualTo(neighbors, WHITE) && board.countEqual(corners, BLACK) >= (corners.size>>1)) {
                    false_eyes.push(p);
                }
            }
        }
    }

    return false_eyes;
}
void Goban::fillFalseEyes(const Vec &false_eyes) {
    for (int i=0; i < false_eyes.size; ++i) {
        Vec neighbors;
        Vec dummy;
        board.getNeighbors(false_eyes[i], neighbors);

        place_and_remove(false_eyes[i], (Color)board[neighbors[0]], dummy);
        //board[false_eyes[i]] = board[neighbors[0]];
    }
}

bool Goban::is_territory(Point pt, Color player) {
    Vec         tocheck;
    Vec neighbors;
    int         visited_counter = ++last_visited_counter;
    int         adjacent_player_stones = 0;

    tocheck.push(pt);
    global_visited[pt] = visited_counter;

    while (tocheck.size) {
        Point p = tocheck.remove(0);
        if ((*this)[p] == 0) {
            board.getNeighbors(p, neighbors);
            for (int i=0; i < neighbors.size; ++i) {
                Point neighbor = neighbors[i];
                if (global_visited[neighbor] == visited_counter) continue;
                global_visited[neighbor] = visited_counter;
                tocheck.push(neighbor);
            }
        } else {
            if ((*this)[p] != player) {
                return false;
            }
            adjacent_player_stones++;
        }
    }

    /* if adjacent_player_stones is 0 then we have a blank board */
    return adjacent_player_stones > 0;
}
void Goban::fill_territory(Point pt, Color player) {
    Vec         tocheck;
    Vec neighbors;
    int         visited_counter = ++last_visited_counter;

    tocheck.push(pt);
    global_visited[pt] = visited_counter;

    while (tocheck.size) {
        Point p = tocheck.remove(0);
        if ((*this)[p] == 0) {
            (*this)[p] = player;
            board.getNeighbors(p, neighbors);
            for (int i=0; i < neighbors.size; ++i) {
                Point neighbor = neighbors[i];
                if (global_visited[neighbor] == visited_counter) continue;
                global_visited[neighbor] = visited_counter;
                tocheck.push(neighbor);
            }
        }
    }
}
void Goban::play_out_position(Color player_to_move, const Grid &life_map, const Grid &seki) {
    do_ko_check = 0;
    possible_ko = Point(-1,-1);

    Vec possible_moves;
    Vec illegal_moves;

    for (int y=0; y < height; ++y) {
        for (int x=0; x < width; ++x) {
            Point p(x,y);
            if (board[p] == 0 && seki[p] == 0 && life_map[p] == 0) {
                possible_moves.push(Point(x,y));
            }
        }
    }

    int sanity = 1000;
    int passed = false;
    while (possible_moves.size > 0 && --sanity > 0) {
        int move_idx = rand() % possible_moves.size;
        Point mv(possible_moves[move_idx]);

        if (is_eye(mv, player_to_move)) {
            illegal_moves.push(possible_moves.remove(move_idx));

            if (possible_moves.size == 0) {
                if (passed) {
                    break;
                }
                passed = true;
                possible_moves += illegal_moves;
                illegal_moves.size = 0;
                player_to_move = (Color)-player_to_move;
            }
            continue;
        }


        int result = place_and_remove(mv, player_to_move, possible_moves);
        if (result == OK) {
            passed = false;
            possible_moves.remove(move_idx);
            player_to_move = (Color)-player_to_move;
            possible_moves += illegal_moves;
            illegal_moves.size = 0;
            continue;
        }
        else if (result == ILLEGAL) {
            illegal_moves.push(possible_moves.remove(move_idx));

            if (possible_moves.size == 0) {
                if (passed) {
                    break;
                }
                passed = true;
                possible_moves += illegal_moves;
                illegal_moves.size = 0;
                player_to_move = (Color)-player_to_move;
            }

            continue;
        }
    }
}
Goban::Result Goban::place_and_remove(Point move, Color player, Vec &possible_moves) {
    if (do_ko_check) {
        if (move == possible_ko) {
            return ILLEGAL;
        }
    }

    bool        reset_ko_check = true;
    bool        removed        = false;
    Vec neighbors;

    board.getNeighbors(move, neighbors);

    (*this)[move] = player;
    ++last_visited_counter;
    for (int i=0; i < neighbors.size; ++i) {
        if ((*this)[neighbors[i]] == -player) {
            if (
                /* it's common that a previous has_liberties covers what we're
                 * about to test, so don't double test */
                    global_visited[neighbors[i]] != last_visited_counter
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
    global_visited[pt] = my_visited_counter;

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
            if (c == my_color && global_visited[neighbor.y][neighbor.x] != my_visited_counter) {
                global_visited[neighbor.y][neighbor.x] = my_visited_counter;
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
            if (c == my_color && global_visited[neighbor.y][neighbor.x] != my_visited_counter) {
                global_visited[neighbor.y][neighbor.x] = my_visited_counter;
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
            if (c == my_color && global_visited[neighbor.y][neighbor.x] != my_visited_counter) {
                global_visited[neighbor.y][neighbor.x] = my_visited_counter;
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
            if (c == my_color && global_visited[neighbor.y][neighbor.x] != my_visited_counter) {
                global_visited[neighbor.y][neighbor.x] = my_visited_counter;
                ++tocheck.size;
            }
        }
    }


    return false;
}
int  Goban::remove_group(Point move, Vec &possible_moves) {
    Grid        visited;
    Vec         tocheck;
    Vec         neighbors;
    int         n_removed = 0;
    int         my_color  = (*this)[move];

    tocheck.push(move);
    visited[move] = true;

    while (tocheck.size) {
        Point p = tocheck.remove(0);

        (*this)[p] = 0;
        possible_moves.push(p);
        n_removed++;

        board.getNeighbors(p, neighbors);

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
bool Goban::is_eye(Point pt, Color player) const {
    if ((pt.x == 0        || board[pt.y][pt.x-1] == player) &&
        (pt.x == width-1  || board[pt.y][pt.x+1] == player) &&
        (pt.y == 0        || board[pt.y-1][pt.x] == player) &&
        (pt.y == height-1 || board[pt.y+1][pt.x] == player))
    {
#if 1
        Vec corners;
        board.getCornerPoints(pt, corners);
        if (board.countEqual(corners, -player) >= (corners.size >> 1)) {
            /* False eye */
            return false;
        }
#endif

        return true;
    }
    return false;
}
bool Goban::is_safe_horseshoe(Point pt, Color player) const {
    Vec neighbors;
    board.getNeighbors(pt, neighbors);


    if (board.countEqual(neighbors, player) >= neighbors.size - 1 && board.countEqual(neighbors, -player) == 0) {
        Vec corners;
        board.getCornerPoints(pt, corners);

        if (board.countEqual(corners, -player) >= (corners.size >> 1)) {
            /* Looks like a false eye, or very close to it */
            return false;
        }

        return true;
    }

    return false;
}
void Goban::setSize(int width, int height) {
    this->width = width;
    this->height = height;
}
void Goban::clearBoard() {
    board.clear();
}