cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

set(ESTIMATOR_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../shared/src/nativeInterop/cpp)

add_library(estimator SHARED
        src/main/cpp/jnibindings.cpp
        ${ESTIMATOR_DIR}/estimator.cpp
        )
target_include_directories(estimator PRIVATE ${ESTIMATOR_DIR})
