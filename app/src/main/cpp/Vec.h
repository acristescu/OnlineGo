#pragma once

#include "Point.h"
#include "constants.h"

class Vec {
    public:
        Point points[MAX_VEC_SIZE];
        int size;

    public:
        Vec() : size(0) {
        }
        Vec(const Vec &o) : size(o.size) {
            for (int i = 0; i < size; ++i) {
                points[i] = o.points[i];
            }
        }
        Point operator[](const int &i) const { 
            return points[i]; 
        }
        Point& operator[](const int &i) { 
            return points[i]; 
        }
        void push(const Point &p) {
            points[size++] = p;
        }
        Point remove(int idx) {
            Point ret = points[idx];
            points[idx] = points[--size];
            return ret;
        }
        Vec& operator+=(const Vec &o) {
            for (int i=0; i < o.size; ++i) {
                push(o[i]);
            }
            return *this;
        }
};


#ifdef DEBUG
#  include "constants.h"
#  include <iostream>

inline std::ostream& operator<<(std::ostream &o, const Vec &vec) {
    for (int i=0; i < vec.size; ++i) {
        if (i > 0) {
            o << ", ";
        }
        o << vec[i];
    }
    return o;
}
#endif