#pragma once

#include "constants.h"
#include "Point.h"
#include "Vec.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/* Simple 2d array used for various purposes while tracking game and estimation state */

template<typename T=int, int MAX_W=MAX_WIDTH, int MAX_H=MAX_HEIGHT>
class TGrid {
public:
    int width;
    int height;

    TGrid(int width=-1, int height=-1)
            : width(width <= 0 ? default_grid_width : width)
            , height(height < 0 ? default_grid_height : height)
    {
        clear();
    }

    inline T* operator[](const int &y) { return _data[y]; }
    inline const T* operator[](const int &y) const { return _data[y]; }
    inline T at(const Point &p) const { return _data[p.y][p.x]; }
    inline T& at(const Point &p) { return _data[p.y][p.x]; }
    inline T operator[](const Point &p) const { return _data[p.y][p.x]; }
    inline T& operator[](const Point &p) { return _data[p.y][p.x]; }
    inline TGrid operator+(const TGrid &o) const {
        TGrid ret(width, height);
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                ret[y][x] = _data[y][x] + o._data[y][x];
            }
        }
        return ret;
    }
    inline TGrid& operator+=(const TGrid &o) {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                _data[y][x] += o._data[y][x];
            }
        }
        return *this;
    }
    inline TGrid& operator*=(const TGrid &o) {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                _data[y][x] *= o._data[y][x];
            }
        }
        return *this;
    }
    inline TGrid& operator*=(const T &v) {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                _data[y][x] *= v;
            }
        }
        return *this;
    }
    inline TGrid operator*(const TGrid &o) const {
        TGrid ret(*this);
        ret *= o;
        return ret;
    }
    inline TGrid operator*(const T &v) const {
        TGrid ret(*this);
        ret *= v;
        return ret;
    }

    /* Clears all locations with the provided value */
    void clear(const T &value=0) {
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                _data[y][x] = value;
            }
        }
    }

    /* Flood matches all similar values starting at the starting_point, writes value to
     * the corresponding coordinates int he destination grid. */
    void traceGroup(const Point &starting_point, TGrid &destination, const T &value) const {
        Vec         tocheck;
        Vec         neighbors;
        TGrid<int>  visited(width, height);
        T           matching_value = (*this)[starting_point];

        tocheck.push(starting_point);
        visited[starting_point] = 1;

        while (tocheck.size) {
            Point p = tocheck.remove(0);
            if ((*this)[p] == matching_value) {
                destination[p] = value;
                getNeighbors(p, neighbors);
                for (int i=0; i < neighbors.size; ++i) {
                    Point neighbor = neighbors[i];
                    if (visited[neighbor]) {
                        continue;
                    }
                    visited[neighbor] = 1;
                    tocheck.push(neighbor);
                }
            }
        }
    }

    /* Flood matches all similar values starting at starting_point, writing all points
     * within the flood match to group, and all neighboring points to neighbors */
    void groupAndNeighbors(const Point &starting_point, Vec &group, Vec &out_neighbors) const {
        Vec         tocheck;
        Vec         neighbors;
        TGrid<int>  visited(width, height);
        T           matching_value = (*this)[starting_point];

        tocheck.push(starting_point);
        visited[starting_point] = 1;

        while (tocheck.size) {
            Point p = tocheck.remove(0);
            if ((*this)[p] == matching_value) {
                group.push(p);
                getNeighbors(p, neighbors);
                for (int i=0; i < neighbors.size; ++i) {
                    Point neighbor = neighbors[i];
                    if (visited[neighbor]) {
                        continue;
                    }
                    visited[neighbor] = 1;
                    tocheck.push(neighbor);
                }
            } else {
                out_neighbors.push(p);
            }
        }
    }

    /* Flood matches all similar values starting at starting_point, writing all points
     * within the flood match to group. */
    Vec group(const Point &starting_point) const {
        Vec         tocheck;
        Vec         ret;
        Vec         neighbors;
        TGrid<int>  visited(width, height);
        T           matching_value = (*this)[starting_point];

        tocheck.push(starting_point);
        visited[starting_point] = 1;

        while (tocheck.size) {
            Point p = tocheck.remove(0);
            if ((*this)[p] == matching_value) {
                ret.push(p);
                getNeighbors(p, neighbors);
                for (int i=0; i < neighbors.size; ++i) {
                    Point neighbor = neighbors[i];
                    if (visited[neighbor]) {
                        continue;
                    }
                    visited[neighbor] = 1;
                    tocheck.push(neighbor);
                }
            }
        }

        return ret;
    }

    /* Returns a Vec containing all the points in group which are equal to value */
    Vec match(const Vec &group, const T &value) const {
        Vec ret;
        for (int i=0; i < group.size; ++i) {
            if (at(group[i]) == value) {
                ret.push(group[i]);
            }
        }
        return ret;
    }

    /* Returns a Vec containing all the points in group which are not equal to value */
    Vec notMatch(const Vec &group, const T &value) const {
        Vec ret;
        for (int i=0; i < group.size; ++i) {
            if (at(group[i]) != value) {
                ret.push(group[i]);
            }
        }
        return ret;
    }

    /* Returns the maximum or minimum value of all points within the
     * provided group, whichever has the greatest magnitude */
    T minmax(const Vec &group) const {
        T ret = (*this)[group[0]];
        for (int i=0; i < group.size; ++i) {
            if (abs(ret) < abs((*this)[group[i]])) {
                ret = (*this)[group[i]];
            }
        }
        return ret;
    }
    T min(const Vec &group) const {
        T ret = (*this)[group[0]];
        for (int i=0; i < group.size; ++i) {
            if (ret > (*this)[group[i]]) {
                ret = (*this)[group[i]];
            }
        }
        return ret;
    }
    T max(const Vec &group) const {
        T ret = (*this)[group[0]];
        for (int i=0; i < group.size; ++i) {
            if (ret < (*this)[group[i]]) {
                ret = (*this)[group[i]];
            }
        }
        return ret;
    }

    /* Sets all points in group to the provided value */
    void set(const Vec &group, const T &value) {
        for (int i=0; i< group.size; ++i) {
            (*this)[group[i]] = value;
        }
    }

    /* Returns true if all points in the provided group have the given value*/
    bool allEqualTo(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) != value) {
                return false;
            }
        }
        return true;
    }

    /* Returns true if all points in the provided group have the given value*/
    bool allNotEqualTo(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) == value) {
                return false;
            }
        }
        return true;
    }

    /* Returns true if the absolute value of at least one point in the provided group are less than or equal to the given value*/
    bool anyAbsLTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (abs(at(group[i])) <= value) {
                return true;
            }
        }
        return false;
    }

    /* Returns true if the absolute value of at least one point in the provided group are greater than or equal to the given value*/
    bool anyAbsGTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (abs(at(group[i])) >= value) {
                return true;
            }
        }
        return false;
    }

    /* Returns true if the absolute value of all points in the provided group are less than or equal to the given value*/
    bool allAbsLTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (abs(at(group[i])) > value) {
                return false;
            }
        }
        return true;
    }

    /* Returns true if the absolute value of all points in the provided group are greater than or equal to the given value*/
    bool allAbsGTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (abs(at(group[i])) < value) {
                return false;
            }
        }
        return true;
    }


    /* Returns true if the value of at least one point in the provided group are less than or equal to the given value*/
    bool anyLTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) <= value) {
                return true;
            }
        }
        return false;
    }

    /* Returns true if the value of at least one point in the provided group are greater than or equal to the given value*/
    bool anyGTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) >= value) {
                return true;
            }
        }
        return false;
    }

    /* Returns true if the value of all points in the provided group are less than or equal to the given value*/
    bool allLTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) > value) {
                return false;
            }
        }
        return true;
    }

    /* Returns true if the value of all points in the provided group are greater than or equal to the given value*/
    bool allGTE(const Vec &group, const T &value) const {
        for (int i=0; i< group.size; ++i) {
            if (at(group[i]) < value) {
                return false;
            }
        }
        return true;
    }



    /* Returns the count of all points that are equal to the given value */
    int countEqual(const Vec &group, const T &value) const {
        int ret = 0;
        for (int i=0; i< group.size; ++i) {
            ret += at(group[i]) == value;
        }
        return ret;
    }

    /* Writes all valid neighboring points into output */
    void getNeighbors(const Point &pt, Vec &output) const {
        output.size = 0;
        if (pt.x > 0)        output.push(Point(pt.x-1, pt.y));
        if (pt.x+1 < width)  output.push(Point(pt.x+1, pt.y));
        if (pt.y > 0)        output.push(Point(pt.x, pt.y-1));
        if (pt.y+1 < height) output.push(Point(pt.x, pt.y+1));
    }

    /* Writes all valid kitty corner neighboring points into output */
    void getCornerPoints(const Point &pt, Vec &output) const {
        output.size = 0;
        if (pt.x > 0       && pt.y > 0)          output.push(Point(pt.x - 1, pt.y - 1));
        if (pt.x+1 < width && pt.y > 0)          output.push(Point(pt.x + 1, pt.y - 1));

        if (pt.x > 0       && pt.y + 1 < height) output.push(Point(pt.x - 1, pt.y + 1));
        if (pt.x+1 < width && pt.y + 1 < height) output.push(Point(pt.x + 1, pt.y + 1));
    }

    /* Sums up all points */
    T sum() const {
        T total = 0;
        for (int y=0; y < height; ++y) {
            for (int x=0; x < width; ++x) {
                total += _data[y][x];
            }
        }
        return total;
    }

    /* Sums up all points in a group */
    T sum(const Vec &group) const {
        T total = 0;
        for (int i=0; i < group.size; ++i) {
            total += _data[group[i].y][group[i].x];
        }
        return total;
    }

private:
    T   _data[MAX_H][MAX_W];

#ifdef DEBUG
    public:
        void print(unsigned char black='X', unsigned char white='o', unsigned char blank='.') const {
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
                printf("%2d|", height-y);
                for (int x=0; x < width; ++x) {
                    printf(" %c", _data[y][x] == 0 ? blank : (_data[y][x] == 1 ? black: white));
                }
                printf(" |%-2d\n", height-y);
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
        void printInts(const char *digit_format=" %3d", const char *spaces="   ") const {
            printf("    ");
            for (int x=0; x < width; ++x) {
                printf("%s%c", spaces, board_letters[x]);
            }
            printf("  \n");

            printf("  ");
            for (int x=0; x <= width; ++x) {
                printf("%c", x == 0 ? '+' : '-');
                for (unsigned i=0; i < strlen(spaces); ++i) {
                    printf("%c", '-');
                }
            }
            printf("+  \n");

            for (int y=0; y < height; ++y) {
                printf("%2d| ", height-y);
                for (int x=0; x < width; ++x) {
                    printf(digit_format, (int)_data[y][x]);
                }
                printf(" |%-2d\n", height-y);
            }

            printf("  ");
            for (int x=0; x <= width; ++x) {
                printf("%c", x == 0 ? '+' : '-');
                for (unsigned i=0; i < strlen(spaces); ++i) {
                    printf("%c", '-');
                }
            }
            printf("+  \n");

            printf("    ");
            for (int x=0; x < width; ++x) {
                printf("%s%c", spaces, board_letters[x]);
            }
            printf("  \n");
        }

#endif
};

typedef TGrid<int> Grid;