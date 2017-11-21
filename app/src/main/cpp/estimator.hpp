#ifndef ESTIMATOR_H
#define ESTIMATOR_H
/* vim: set tabstop=4 expandtab foldmethod=marker: */

#ifdef DEBUG
#  include <iostream>
using namespace std;
#endif

#define MAX_WIDTH 25
#define MAX_HEIGHT 25
#define MAX_SIZE (MAX_WIDTH*MAX_HEIGHT)

namespace score_estimator {

enum Color {
    EMPTY = 0,
    BLACK = 1,
    WHITE = -1
};

inline Color other(Color c) { return c == BLACK ? WHITE : BLACK; }

class Point {
    public:
        int x,y;
        Point(){}
        Point(int _x, int _y=0) : x(_x), y(_y) {}
        bool operator==(const Point &p) const { return x == p.x && y == p.y; }
        bool operator!=(const Point &p) const { return x != p.x || y != p.y; }
};

template<int SIZE>
class Vec_t {
    public:
        Point points[SIZE];
        int size;

    public:
        Vec_t() {
            size = 0;
        }
        Point operator[](const int &i) const { return points[i]; }
        Point& operator[](const int &i) { return points[i]; }
        void push(const Point &p) {
            points[size++] = p;
        }
        Point remove(int idx) {
            Point ret = points[idx];
            points[idx] = points[--size];
            return ret;
        }

};

typedef Vec_t<MAX_SIZE> Vec;
typedef Vec_t<4> NeighborVec;

enum Result {
    OK = 0,
    ILLEGAL = 1,
};

class Goban {
    public:
        int   width;
        int   height;
        int   board[MAX_HEIGHT][MAX_WIDTH];
        int   do_ko_check;
        Point possible_ko;

        Goban();
        Goban(const Goban &other);
        Goban estimate(Color player_to_move, int trials, float tolerance);
        Point generateMove(Color player, int trials, float tolerance);
        inline int operator[](const Point &p) const { return board[p.y][p.x]; }
        inline int& operator[](const Point &p) { return board[p.y][p.x]; }
        int score();
        void setSize(int width, int height);
        void clearBoard();
        void play_out_position(Color player_to_move);
        Result place_and_remove(Point move, Color player, Vec &possible_moves);

    private:
        void init();
        void get_neighbors(const Point &pt, NeighborVec &output);
        bool has_liberties(const Point &pt);
        int  remove_group(Point move, Vec &possible_moves);
        bool is_eye(Point move, Color player);
        bool is_territory(Point pt, Color player);
        void fill_territory(Point pt, Color player);
        void synchronize_tracking_counters(int track[MAX_HEIGHT][MAX_WIDTH], Goban &visited, Point &p);


#ifdef DEBUG
    public:
        void print();
        Point pointFromStr(const char *str);
        void showBoard();
#endif
};


#ifdef DEBUG
ostream& operator<<(ostream &o, const Point &pt);
#endif


} /* namespace baduk */

#endif /* ESTIMATOR_H */
