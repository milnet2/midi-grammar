cmake_minimum_required(VERSION 3.22)
project(midi_file_grammar)

if(NOT UNIX)
  message(WARNING "Unsupported operating system")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

include(FetchContent)
include(ExternalProject)
FetchContent_Declare(
        antlr4
        URL ${ANTLR4_ZIP_REPOSITORY})
FetchContent_MakeAvailable(antlr4)
include_directories(${antlr4_SOURCE_DIR}/runtime/Cpp/runtime/src/)   # set by FetchContent_MakeAvailable

include_directories(${ANTLR_SampleGrammarLexer_OUTPUT_DIR})
include_directories(${ANTLR_SampleGrammarParser_OUTPUT_DIR})
include_directories(${GENERATED_GRAMMAR_DIR})

add_library(midi_file_grammar STATIC
        ${GENERATED_GRAMMAR_DIR}/Midi1FileLexer.cpp
        ${GENERATED_GRAMMAR_DIR}/Midi1FileParserBaseListener.cpp
        ${GENERATED_GRAMMAR_DIR}/Midi1FileParserBaseVisitor.cpp
        ${GENERATED_GRAMMAR_DIR}/Midi1FileParser.cpp
        ${GENERATED_GRAMMAR_DIR}/Midi1FileParserListener.cpp
        ${GENERATED_GRAMMAR_DIR}/Midi1FileParserVisitor.cpp
        # ${PROJECT_SOURCE_DIR}/LexerAdapter.cpp
)