cmake_minimum_required(VERSION 3.22.1)

project("oboe-engine")

# Locate Android system logging library
find_library(log-lib log)

# Compile Oboe JNI C++ spectral processing engine
add_library(
    oboe-engine
    SHARED
    oboe_engine.cpp
)

target_link_libraries(
    oboe-engine
    ${log-lib}
)
