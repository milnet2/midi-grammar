cmake_minimum_required(VERSION 3.22)
project(midi_file_grammar)

# Only for auxiliary purposes
# The proper settings will be set by pom.xml

set(GENERATED_GRAMMAR_DIR ${PROJECT_SOURCE_DIR}/target/generated-sources/antlr4/de/tobiasblaschke/lib/midi/adapter/grammar/)
set(ANTLR4_TAG 4.13.2)
set(ANTLR4_ZIP_REPOSITORY https://github.com/antlr/antlr4/archive/refs/tags/4.13.2.zip)
set(HERE ${PROJECT_SOURCE_DIR}/src/main/cpp/)
set(TEST_HERE ${PROJECT_SOURCE_DIR}/src/test/cpp/)

include(${HERE}/CMakeLists.txt)
include(${TEST_HERE}/CMakeLists.txt)
