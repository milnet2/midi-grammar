cmake_minimum_required(VERSION 3.22)
project(midi_file_grammar)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

# include_directories(${PROJECT_SOURCE_DIR}/src/include)

add_library(midi_file_grammar STATIC
        ${PROJECT_SOURCE_DIR}/LexerAdapter.cpp)