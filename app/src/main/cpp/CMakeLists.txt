
cmake_minimum_required(VERSION 3.4.1)

# Add FindAndroidThings.cmake dir to the module path.
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH}
    ${PROJECT_DIR}/libandroidthings)
# Resolve Android Things headers and libraries
find_package(AndroidThings REQUIRED)

add_library(dht SHARED
            dht.cpp)

target_include_directories(dht PRIVATE
                           ${PROJECT_DIR}/android-system-properties
                           ${ANDROID_NDK}/sources/android/native_app_glue
                           ${ANDROIDTHINGS_INCLUDE_DIRS})

add_library(android-system-properties STATIC
            ${PROJECT_DIR}/android-system-properties/AndroidSystemProperties.cpp)
target_include_directories(android-system-properties PRIVATE
                           ${ANDROID_NDK}/sources/android/native_app_glue
                           ${ANDROIDTHINGS_INCLUDE_DIRS})

add_library(native-app-glue STATIC
            ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c)

add_library(androidthings SHARED
            IMPORTED)
set_target_properties(androidthings
                      PROPERTIES IMPORTED_LOCATION
                      ${ANDROIDTHINGS_LIBRARIES})

target_link_libraries(dht
                      android
                      log
                      native-app-glue
                      androidthings
                      android-system-properties)