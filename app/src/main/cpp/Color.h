#pragma once

enum Color {
    EMPTY = 0,
    BLACK = 1,
    WHITE = -1
};

inline Color other(Color c) { 
    return c == BLACK ? WHITE : BLACK; 
}