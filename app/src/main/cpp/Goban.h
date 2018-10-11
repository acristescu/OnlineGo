#pragma once

#include "Color.h"
#include "Point.h"
#include "Vec.h"
#include "Grid.h"
#ifdef USE_THREADS
#  include <random>
#endif

class Goban {
public:
    enum Result {
        OK = 0,
        ILLEGAL = 1,
    };

public:
    int       width;
    int       height;
    Grid      board;
    int       do_ko_check;
    Point     possible_ko;

    Grid      global_visited;
    int       last_visited_counter;

#ifdef USE_THREADS
    std::mt19937 rand;
#endif

    Goban(int width, int height);
    Goban(const Goban &other);
    void setBoardSize(int width, int height);
    Grid estimate(Color player_to_move, int trials, float tolerance, bool debug) const;
    Point generateMove(Color player, int trials, float tolerance);
    inline int at(const Point &p) const { return board[p]; }
    inline int& at(const Point &p) { return board[p]; }
    inline int operator[](const Point &p) const { return board[p]; }
    inline int& operator[](const Point &p) { return board[p]; }
    void setSize(int width, int height);
    void clearBoard();
    void play_out_position(Color player_to_move, const Grid &life_map, const Grid &seki);
    Result place_and_remove(Point move, Color player, Vec &possible_moves);

    /* Looks for probable seki situations and returns them as a binary grid */
    Grid scanForSeki(int num_iterations, float tolerance, const Grid &rollout_pass) const;

    /** Returns a list of false eyes detected */
    Vec getFalseEyes() const;

    /** Fills false eyes, removing stones if appropriate */
    void fillFalseEyes(const Vec &false_eyes);
    void fillFalseEyes() { fillFalseEyes(getFalseEyes()); }

    /**
     * Play num_iterations random matches. Don't play in places identifies
     * by life_map, assuming those spots are definitely alive and non
     * invadable. Note this is a heavy bias on not assuming territory is
     * invadable, which for our purposes is fine. Such things would be
     * horrible for a bot, but we're just trying to mark the board up how
     * the players, who may be weak or strong, view the board.
     */
    Grid rollout(int num_iterations, Color player_to_move, bool pullup_life_based_on_neigboring_territory = true, const Grid &life_map = Grid(), const Grid &bias = Grid(), const Grid &seki = Grid()) const;

    /**
     * We bias positions on the board based on who they currently belong
     * to. The goal with the bias is to help prevent unexpected removals of
     * tricky-but-still-alive stones while still not hindering the removal
     * of obviously dead bad invasions or obviously dead groups.
     */
    Grid computeBias(int num_iterations, float tolerance);

    /**
     * Bias against some probably very dead stone groups, namely groups with
     * no eyes that are surrounded by area that is almost definitely the opponents
     */
    Grid biasLikelyDead(int num_iterations, float tolerance, const Grid &liberty_map) const;

    Grid biasLibertyMap(int num_iterations, float tolerance, const Grid &liberty_map) const;

    /**
     * Marks each location with the positive or negative size of the
     * territory (negative for white, postive for black), or zero if the
     * location is not territory. */
    Grid computeTerritory();

    /** Uniquely labels strings of groups on the board. */
    Grid computeGroupMap() const;

    /**
     * Computes the liberties for any groups on the boards. For empty
     * spaces, computes the number of blank minus number of white stones
     * touching the group. The value is always negative for white and positive for black.
     */
    Grid computeLiberties(const Grid &group_map) const;

    /**
     * Flags spaces that are part of a string of like colored stone strings
     * and territory * so long as the stone strings have a combined two or
     * more territory
     */
    Grid computeStrongLife(const Grid &groups, const Grid &territory, const Grid &liberties) const;

    /**
     * Returns a list of stones that are probably dead as determined by
     * looking at the results of a rollout pass compared to our initial
     * board state
     */
    Vec getDead(int num_iterations, float tolerance, const Grid &rollout_pass) const;

private:
    Grid _estimate(Color player_to_move, int trials, float tolerance, bool debug);
    bool has_liberties(const Point &pt);
    int  remove_group(Point move, Vec &possible_moves);
    bool is_eye(Point move, Color player) const;
    bool is_safe_horseshoe(Point move, Color player) const; // u shape but not eye, without opponents in enough corners to be dangerous
    bool is_territory(Point pt, Color player) ;
    void fill_territory(Point pt, Color player);


#if 0
    void synchronize_tracking_counters(Grid &track, Goban &visited, Point &p);
#endif
};