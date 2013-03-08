#! /bin/bash
#############################################################################
# Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática  #
# Departamento de Engenharia Electrotécnica e de Computadores               #
# Rua Dr. Roberto Frias, 4200-465 Porto, Portugal                           #
#############################################################################
# Author: Ricardo Martins                                                   #
#############################################################################
# $Id:: addsvnkeywords.sh 9615 2012-12-30 23:08:28Z pdias                 $:#
#############################################################################
# This script will add the svn:keywords property to all files in the        #
# repository.                                                               #
#############################################################################

find src plugins-dev -type f -name '*.java'  | grep -v svn | while read file; do
    svn propset svn:keywords Id "$file"
done

find . -type f -name '*.sh'  | grep -v svn | while read file; do
    svn propset svn:keywords Id "$file"
done
find . -type f -name '*.bat'  | grep -v svn | while read file; do
    svn propset svn:keywords Id "$file"
done
find . -type f -name '*.xml'  | grep -v svn | while read file; do
    svn propset svn:keywords Id "$file"
done
