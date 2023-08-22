cmake_minimum_required(VERSION 3.25)

project(javascalpel C)

set(CMAKE_C_STANDARD 99)
find_package(JNI REQUIRED)

add_library(javascalpel SHARED javascalpel.c)
target_link_libraries(javascalpel PRIVATE JNI::JNI)
if (WIN32)
    target_link_libraries(javascalpel PRIVATE Kernel32)
else ()
    target_link_libraries(javascalpel PRIVATE ${CMAKE_DL_LIBS})
endif ()
