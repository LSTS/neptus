# -*- cmake -*-
cmake_minimum_required(VERSION 2.6.0 FATAL_ERROR)
set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/cmake/Modules/")
project(neptus_europa)

set(PROJECT_VENDOR "Universidade do Porto - LSTS")
set(PROJECT_COPYRIGHT "Copyright (C) 2014 - ${PROJECT_VENDOR}")
set(PROJECT_MAJOR 0)
set(PROJECT_MINOR 1)
set(PROJECT_PATCH 0)

set(VERSION "${PROJECT_MAJOR}.${PROJECT_MINOR}.${PROJECT_PATCH}")

set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Europa extensions for neptus integration")
set(CPACK_PACKAGE_VENDOR "${PROJECT_VENDOR}")
set(CPACK_PACKAGE_DESCRIPTION_FILE ${CMAKE_CURRENT_SOURCE_DIR}/README)
set(CPACK_PACKAGE_VERSION ${VERSION})

set(
CPACK_SOURCE_PACKAGE_FILE_NAME
"${CMAKE_PROJECT_NAME}-${VERSION}"
CACHE INTERNAL "Package basename"
)

set(CPACK_SOURCE_IGNORE_FILES
"~$"
"/\\\\.svn/"
"\\\\.DS_Store$"
)

include(CPack)

########################################################################
# System libraries                                                     #
########################################################################
include(CheckLibraryExists)

check_library_exists(dl dlopen "" LIB_DL)
if(LIB_DL)
  set(SYSTEM_LIBRARIES ${SYSTEM_LIBRARIES} dl)
else(LIB_DL) 
  message(FATAL_ERROR "TREX requires lib dl")  
endif(LIB_DL)

#######################################################################
# Look for dune
#######################################################################

set(DUNE_HOME $ENV{DUNE_HOME} CACHE PATH "Expected location for dune")

find_library(DUNE_CORE_LIB NAMES dune-core HINTS ${DUNE_HOME}/lib
  DOCS "Looking for dune core library")

if(NOT DUNE_CORE_LIB)
  message(ERROR "Failed to locate dune-core library\n"
    "Please set tyur DUNE_HOME where dune is installed.")
endif(NOT DUNE_CORE_LIB)

find_path(DUNE_INCLUDE "DUNE/DUNE.hpp" HINTS ${DUNE_HOME}/include)

if(NOT DUNE_INCLUDE)
  message(ERROR "Failed to locate dune-core headers\n"
    "Please set tyur DUNE_HOME where dune is installed.")
endif(NOT DUNE_INCLUDE)

include_directories(${DUNE_INCLUDE})

#######################################################################
# Look for Europa
#######################################################################

find_package(Europa
  COMPONENTS PlanDatabase ConstraintEngine TemporalNetwork Utils TinyXml 
  NDDL RulesEngine Solvers System)

if(NOT EUROPA_FOUND)
  message(FATAL_ERROR "Failed to find Europa while WITH_EUROPA is ON")
endif(NOT EUROPA_FOUND)


include_directories(${EUROPA_INCLUDE_DIRS} src)

add_library(Neptus SHARED
  src/module.cc
  src/LatLonDist.cc
  src/sqrt.cc
  src/to_float.cc
  src/LatLonDist.hh
  src/sqrt.hh
  src/to_float.hh)

set_target_properties(Neptus PROPERTIES 
  COMPILE_DEFINITIONS "${EUROPA_FLAGS}")


target_link_libraries(Neptus
  ${EUROPA_LIBRARIES}
  ${DUNE_CORE_LIB}
  ${SYSTEM_LIBRARIES})