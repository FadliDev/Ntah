cmake_minimum_required(VERSION 3.22.1)
project(sallyhome)
add_library(sallyhome SHARED native-lib.cpp)
target_link_libraries(sallyhome)
