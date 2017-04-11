set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} <path_to_FindAndroidThings_directory.cmake>)
find_package(AndroidThings REQUIRED)
include_directories(${ANDROIDTHINGS_INCLUDE_DIRS})
target_link_libraries(<your_target_name> ${ANDROIDTHINGS_LIBRARIES})
