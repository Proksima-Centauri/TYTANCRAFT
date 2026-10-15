cmake_minimum_required(VERSION 3.16)
project(tytancraft_world LANGUAGES CXX)

find_package(JNI REQUIRED)

add_library(tytancraft_world SHARED worldgen.cpp)
target_include_directories(tytancraft_world PRIVATE ${JNI_INCLUDE_DIRS})
target_compile_features(tytancraft_world PRIVATE cxx_std_17)
set_target_properties(tytancraft_world PROPERTIES CXX_VISIBILITY_PRESET hidden)
