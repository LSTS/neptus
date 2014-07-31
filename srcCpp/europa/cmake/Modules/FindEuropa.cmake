# -*- cmake -*- 
#######################################################################
# Software License Agreement (BSD License)                            #
#                                                                     #
#  Copyright (c) 2011, MBARI.                                         #
#  All rights reserved.                                               #
#                                                                     #
#  Redistribution and use in source and binary forms, with or without #
#  modification, are permitted provided that the following conditions #
#  are met:                                                           #
#                                                                     #
#   * Redistributions of source code must retain the above copyright  #
#     notice, this list of conditions and the following disclaimer.   #
#   * Redistributions in binary form must reproduce the above         #
#     copyright notice, this list of conditions and the following     #
#     disclaimer in the documentation and/or other materials provided #
#     with the distribution.                                          #
#   * Neither the name of the TREX Project nor the names of its       #
#     contributors may be used to endorse or promote products derived #
#     from this software without specific prior written permission.   #
#                                                                     #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS #
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT   #
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS   #
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE      #
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, #
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,#
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;    #
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER    #
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  #
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN   #
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE     #
# POSSIBILITY OF SUCH DAMAGE.                                         #
#######################################################################

# - Try to find europa-pso
# Once done this will define
#  EUROPA_FOUND        - System has europa
#  EUROPA_INCLUDE_DIRS - The europa include directories
#  EUROPA_LIBRARIES    - The libraries needed to use europa
#  EUROPA_DEFINITIONS  - Compiler switches required for using europa

# basic initial options
option(EUROPA_DEBUG "Compile with Debug variant of Europa" OFF)
option(EUROPA_FORCE_EFFECT 
  "Force the effect support flag for Europa (Usefull when the try_compile is failing even if the europa version is greater than 2.6)" OFF)
mark_as_advanced(EUROPA_FORCE_EFFECT)
set(EUROPA_HINTS $ENV{EUROPA_HOME} CACHE PATH
  "Hint to Europa location (usually $EUROPA_HOME)")

# cached option values to check if any was updated
if(NOT OLD_EUROPA_DEBUG EQUAL EUROPA_BEBUG)
  set(OLD_EUROPA_DEBUG ${EUROPA_DEBUG} CACHE 
    INTERNAL "Former EUROPA_DEBUG value." FORCE)
endif(NOT OLD_EUROPA_DEBUG EQUAL EUROPA_BEBUG)

if(NOT OLD_EUROPA_HINTS EQUAL EUROPA_HINTS)
  set(OLD_EUROPA_HINTS ${EUROPA_HINTS} CACHE 
    INTERNAL "Former EUROPA_HINTS value." FORCE)
endif(NOT OLD_EUROPA_HINTS EQUAL EUROPA_HINTS)
 
set(_europa_LIBRARIES 
  ConstraintEngine Solvers NDDL System PlanDatabase
  TemporalNetwork Resources TinyXml RulesEngine Utils)

if(EUROPA_DEBUG)
  set(_europa_VARIANT "_g") #only look for debug
  set(_europa_FLAGS TIXML_USE_STL)
else(EUROPA_DEBUG)
  set(_europa_VARIANT "_o") #only look for optimized 
  set(_europa_FLAGS TIXML_USE_STL;EUROPA_FAST)
endif(EUROPA_DEBUG)

set(EUROPA_FLAGS ${_europa_FLAGS} CACHE 
  INTERNALS "Flags to compile with Europa" FORCE)

if(NOT Europa_FIND_COMPONENTS)
  # we need everything then
  set(Europa_FIND_COMPONENTS ${_europa_LIBRARIES})
endif(NOT Europa_FIND_COMPONENTS)

set(EUROPA_HOME "" CACHE STRING "europa home path" FORCE)
if(NOT OLD_EUROPA_HOME EQUAL EUROPA_HOME) 
  set(EUROPA_HOME
    "EUROPA_HOME-NOTFOUND"
    CACHE FILEPATH "Cleared." FORCE)
endif(NOT OLD_EUROPA_HOME EQUAL EUROPA_HOME)

find_path(EUROPA_HOME
  "include/Plasma.nddl" HINTS ${EUROPA_HINTS}) 



set(_europa_MISSING     "")
set(EUROPA_LIB_NAMES    "")
set(EUROPA_LIBRARIES    "")
set(EUROPA_LIBRARY_DIRS "")

# Look for the requested libraries
foreach(COMPONENT ${Europa_FIND_COMPONENTS})
  string(TOUPPER ${COMPONENT} UPPERCOMPONENT)
  set(EUROPA_${UPPERCOMPONENT}_LIBRARY "" CACHE 
    STRING "location of ${COMPONENT} europa library" FORCE)
  if(NOT OLD_EUROPA_DEBUG EQUAL EUROPA_BEBUG) 
    set(EUROPA_${UPPERCOMPONENT}_LIBRARY 
      "EUROPA_${UPPERCOMPONENT}_LIBRARY-NOTFOUND"
      CACHE FILEPATH "Cleared." FORCE)
  endif(NOT OLD_EUROPA_DEBUG EQUAL EUROPA_BEBUG)
  find_library(EUROPA_${UPPERCOMPONENT}_LIBRARY
    NAMES ${COMPONENT}${_europa_VARIANT}
    HINTS ${EUROPA_HINTS}/lib
    DOCS "Looking for ${COMPONENT}")
  mark_as_advanced(EUROPA_${UPPERCOMPONENT}_LIBRARY)
  if(EUROPA_${UPPERCOMPONENT}_LIBRARY)
    set(EUROPA_${UPPERCOMPONENT}_NAME ${COMPONENT}${_europa_VARIANT})
    list(APPEND EUROPA_LIBRARIES ${EUROPA_${UPPERCOMPONENT}_LIBRARY})
    get_filename_component(_europa_my_lib_path 
      "${EUROPA_${UPPERCOMPONENT}_LIBRARY}" PATH)
    list(APPEND EUROPA_LIBRARY_DIRS ${_europa_my_lib_path})
  else(EUROPA_${UPPERCOMPONENT}_LIBRARY)
    list(APPEND _europa_MISSING ${COMPONENT})
  endif(EUROPA_${UPPERCOMPONENT}_LIBRARY)
endforeach(COMPONENT ${Europa_FIND_COMPONENTS})

list(REMOVE_DUPLICATES EUROPA_LIBRARY_DIRS)
list(REMOVE_DUPLICATES _europa_MISSING)
list(REMOVE_DUPLICATES EUROPA_LIBRARIES)


set(EUROPA_INCLUDE_DIR
  "" CACHE STRING "location of europa headers" FORCE)

if(NOT OLD_EUROPA_HINTS EQUAL EUROPA_HINTS) 
  set(EUROPA_INCLUDE_DIR
    "EUROPA_INCLUDE_DIR-NOTFOUND"
    CACHE FILEPATH "Cleared." FORCE)
  
endif(NOT OLD_EUROPA_HINTS EQUAL EUROPA_HINTS)
find_path(EUROPA_INCLUDE_DIR
  "PSSolvers.hh" HINTS ${EUROPA_HINTS}/include) 
set(EUROPA_INCLUDE_DIRS ${EUROPA_INCLUDE_DIR} 
  CACHE STRING "Europa include paths" FORCE)


if(EUROPA_FORCE_EFFECT)
  set(EUROPA_HAVE_EFFECT TRUE)
else(EUROPA_FORCE_EFFECT)
  # Check if this version support actions
  file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/europa_26.cc "#include <PLASMA/Token.hh>

bool isEffect(EUROPA::TokenId const &tok) {
  return tok->hasAttributes(EUROPA::PSTokenType::EFFECT);
}

int main() { return 0; }
")

  try_compile(EUROPA_HAVE_EFFECT "${CMAKE_CURRENT_BINARY_DIR}"
    "${CMAKE_CURRENT_BINARY_DIR}/europa_26.cc"
    COMPILE_DEFINITIONS -I${EUROPA_INCLUDE_DIR}
    OUTPUT_VARIABLE OUT)
  file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/europa_26.log "${OUT}")
endif(EUROPA_FORCE_EFFECT)

if(_europa_MISSING OR NOT EUROPA_INCLUDE_DIR)
  set(EUROPA_FOUND FALSE)
  if(Europa_FIND_REQUIRED)
    message(SEND_ERROR "Unable to find requested europa libraries")
  endif(Europa_FIND_REQUIRED)
  message(STATUS "Europa not found\nSet your EUROPA_HOME in your environment")
else(_europa_MISSING OR NOT EUROPA_INCLUDE_DIR)
  set(EUROPA_FOUND TRUE)
  message(STATUS "Europa found : include directory is ${EUROPA_INCLUDE_DIR}")
endif(_europa_MISSING OR NOT EUROPA_INCLUDE_DIR)

mark_as_advanced(EUROPA_HOME
  EUROPA_INCLUDE_DIR
  EUROPA_INCLUDE_DIRS
  EUROPA_LIBRARIES
  EUROPA_FLAGS
  EUROPA_LIB_NAMES
  EUROPA_LIBRARY_DIRS
  EUROPA_HAVE_EFFECT
  )

