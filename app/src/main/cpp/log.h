#ifndef LOG_H
#define LOG_H
#ifndef EMSCRIPTEN

#include "rang.hpp"
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <iostream>
#include <iostream>
#include <fstream>

using namespace rang;

typedef enum log_verbosity_t {
    LOG_NONE     = 0,
    LOG_CRITICAL = 1,
    LOG_ERROR    = 2,
    LOG_WARNING  = 3,
    LOG_NOTE     = 4,
    LOG_INFO     = 5,
    LOG_VERBOSE  = 6,
    LOG_DEBUG    = 7,
    LOG_TRACE    = 8,
} log_verbosity_t;

#define CRITICAL if (log_verbosity < LOG_CRITICAL) {} else _logger(LOG_CRITICAL, __FILE__, __LINE__)
#define ERROR    if (log_verbosity < LOG_ERROR   ) {} else _logger(LOG_ERROR   , __FILE__, __LINE__)
#define WARNING  if (log_verbosity < LOG_WARNING ) {} else _logger(LOG_WARNING , __FILE__, __LINE__)
#define NOTE     if (log_verbosity < LOG_NOTE    ) {} else _logger(LOG_NOTE    , __FILE__, __LINE__)
#define INFO     if (log_verbosity < LOG_INFO    ) {} else _logger(LOG_INFO    , __FILE__, __LINE__)
#define VERBOSE  if (log_verbosity < LOG_VERBOSE ) {} else _logger(LOG_VERBOSE , __FILE__, __LINE__)
#define DBG      if (log_verbosity < LOG_DEBUG   ) {} else _logger(LOG_DEBUG   , __FILE__, __LINE__)
#define TRACE    if (log_verbosity < LOG_TRACE   ) {} else _logger(LOG_TRACE   , __FILE__, __LINE__)

class Logger : public std::basic_ostream<char, std::char_traits< char > >, public std::basic_streambuf<char, std::char_traits<char> > {
    public:
        Logger();
        Logger& operator()(log_verbosity_t verbosity, const char file[], int line);
        Logger& operator()(const char *fmt, ...);

        inline virtual int overflow(int c) override {
            /* Note: we should probably buffer this up instead of doing putc's, then flush in sync() */
            logfile.put(c);
            std::cerr.put(c);
            if (c == '\n') {
                std::cerr << fg::reset;
                needs_newline = false;
            }
            return 0;
        }

        // This function is called when stream is flushed,
        // for example when std::endl is put to stream.
        inline virtual int sync(void) override {
            logfile.flush();
            std::cerr.flush();
            return 0;
        }

    public:
        std::ofstream    logfile;

    private:
        bool             needs_newline;
};


//extern int log_verbosity;
//extern Logger _logger;





int log_verbosity = LOG_VERBOSE;
Logger _logger;

Logger::Logger() : std::basic_ostream<char, std::char_traits<char> >(this), logfile("logfile.txt") {
    //verbosity = LOG_NONE;
    //func = nullptr;
    //line = 0;
    needs_newline = false;;
}
Logger& Logger::operator()(log_verbosity_t verbosity, const char file[], int line) {
    if (needs_newline) {
        (*this) << '\n';
    }

    //this->verbosity = verbosity;
    //this->func = function;
    //this->line = line;

    switch (verbosity) {
        case LOG_NONE     : std::cerr << fg::reset; break ; 
        case LOG_CRITICAL : std::cerr << fg::red; break ; 
        case LOG_ERROR    : std::cerr << fg::red; break ; 
        case LOG_WARNING  : std::cerr << fg::yellow; break ; 
        case LOG_NOTE     : std::cerr << fg::cyan; break ; 
        case LOG_INFO     : std::cerr << fg::reset; break ; 
        case LOG_VERBOSE  : std::cerr << style::dim << fg::gray; break ; 
        case LOG_DEBUG    : std::cerr << fg::blue; break ; 
        case LOG_TRACE    : std::cerr << fg::cyan; break ; 
    }

    switch (verbosity) {
        case LOG_NONE     : (*this) << "n "  ; break ;
        case LOG_CRITICAL : (*this) << "C "  ; break ;
        case LOG_ERROR    : (*this) << "E "  ; break ;
        case LOG_WARNING  : (*this) << "W "  ; break ;
        case LOG_NOTE     : (*this) << "N "  ; break ;
        case LOG_INFO     : (*this) << "I "  ; break ;
        case LOG_VERBOSE  : (*this) << "V "  ; break ;
        case LOG_DEBUG    : (*this) << "D "  ; break ;
        case LOG_TRACE    : (*this) << "T "  ; break ;
    }

    char buf[256];
    int offset = snprintf(buf, sizeof(buf)-1, "%s:%d", file+4 /* +4 to get rid of src/ */, line);
    //(*this)("[%-20s:%5d] \t", file, line);
    (*this)("%-20s| ", buf + MAX(0, offset-20));


    needs_newline = true;

    return *this;
}
Logger& Logger::operator()(const char *fmt, ...) {
    size_t size = 0;
    char *p = nullptr;
    va_list ap;

    /* Determine required size */
    va_start(ap, fmt);
    size = vsnprintf(p, size, fmt, ap);
    va_end(ap);

    if (size < 0) {
        (*this) << "[log error, vsnINFO returned " << size << "]";
        return *this;
    } else {
        size++; /* For '\0' */
        p = (char*)malloc(size);
        if (p == nullptr) {
            (*this) << "[log error, out of memory]";
            return *this;
        } else {

            va_start(ap, fmt);
            size = vsnprintf(p, size, fmt, ap);
            va_end(ap);

            if (size < 0) {
                free(p);
                (*this) << "[log error, vsnINFO returned " << size << "]";
            } else {
                (*this) << p;
                free(p);
            }
        }
    }
    return *this;
}


#endif
#endif /* LOG_H */