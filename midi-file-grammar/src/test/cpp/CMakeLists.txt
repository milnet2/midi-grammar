
include(${TEST_HERE}/CMakeTestDependencies.cmake)

include(FetchContent)
include(ExternalProject)
FetchContent_Declare(
        antlr4
        URL ${ANTLR4_ZIP_REPOSITORY})
FetchContent_MakeAvailable(antlr4)

enable_testing()
add_executable(library_tests
        ${TEST_HERE}/Midi1FileLexerTest.cpp
        ${TEST_HERE}/LexerTestUtils.cpp
        ${TEST_HERE}/LexerTestUtils.h)
target_link_libraries(library_tests gtest gtest_main)
target_link_libraries(library_tests midi_file_grammar)
target_link_libraries(library_tests antlr4)  # <-------------------------------------------

# Add test to CMake's test runner
add_test(NAME LibraryTest COMMAND library_tests)