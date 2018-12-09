#pragma once

class Point {
    public:
        int x;
        int y;

        Point(){
        }

        Point(int _x, int _y) 
            : x(_x), y(_y) 
        {
        }

        bool operator==(const Point &p) const { return x == p.x && y == p.y; }
        bool operator!=(const Point &p) const { return x != p.x || y != p.y; }
        bool operator<(const Point &p) const { return y == p.y ? x < p.x : y < p.y; }
};


#ifdef DEBUG
#  include "constants.h"
#  include <iostream>

inline std::ostream& operator<<(std::ostream &o, const Point &pt) {
    if (pt.x >= 0) {
        o << board_letters[pt.x] << default_grid_height - pt.y;
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