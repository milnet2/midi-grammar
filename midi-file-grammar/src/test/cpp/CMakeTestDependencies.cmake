
include(FetchContent)
include(ExternalProject)

option(USE_SYSTEM_GOOGLETEST "System googletest" ON)
if (${USE_SYSTEM_GOOGLETEST})
#    find_package(googletest REQUIRED)
#    include_directories(${GOOGLETEST_INCLUDE_DIR})
else()
    FetchContent_Declare(
            googletest
            URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.tar.gz)

    set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
    set(BUILD_GMOCK OFF CACHE BOOL "" FORCE)
    set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

    FetchContent_MakeAvailable(googletest)
endif()