#!/bin/bash
#############################################################################
# Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia   #
# Laboratório de Sistemas e Tecnologia Subaquática (LSTS)                   #
# All rights reserved.                                                      #
# Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal            #
#                                                                           #
# This file is part of Neptus, Command and Control Framework.               #
#                                                                           #
# Commercial Licence Usage                                                  #
# Licencees holding valid commercial Neptus licences may use this file      #
# in accordance with the commercial licence agreement provided with the     #
# Software or, alternatively, in accordance with the terms contained in a   #
# written agreement between you and Universidade do Porto. For licensing    #
# terms, conditions, and further information contact lsts@fe.up.pt.         #
#                                                                           #
# Modified European Union Public Licence - EUPL v.1.1 Usage                 #
# Alternatively, this file may be used under the terms of the Modified EUPL,#
# Version 1.1 only (the "Licence"), appearing in the file LICENCE.md        #
# included in the packaging of this file. You may not use this  work        #
# except in compliance with the Licence. Unless required by  applicable     #
# law or agreed to in writing, software distributed under the Licence  is   #
# distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF      #
# ANY KIND, either express or implied. See the Licence for the specific     #
# language governing permissions and limitations at                         #
# http://ec.europa.eu/idabc/eupl.html.                                      #
#                                                                           #
# For more information please see <http://lsts.fe.up.pt/neptus>.            #
#############################################################################
# Author: José Pinto, Paulo Dias                                            #
#############################################################################
# This script will create a tar.gz file with a crash report including some  #
# selected log data.                                                        #
#############################################################################


TEST_ZENITY=$(zenity -h 2>&1 1>/dev/null; echo $?)
if [ "$TEST_ZENITY" -ne 0 ]; then
    echo "Please install Zenity before proceding"
    exit $TEST_ZENITY
fi

PROGNAME=$0
PROGDIRNAME=`dirname $PROGNAME`
LOGSDIR=$PROGDIRNAME/../log
cd $LOGSDIR
LOGSDIR=$(pwd)
input=$(zenity --text "Can you give a description of the error symptoms?" --entry)
retval=$?
case $retval in
    0)
        echo $input > reason.txt ;;
    1)
        exit;;
esac

crfilebasename=CrashReport-$(date +%Y%m%d_%H%M%S_%Z)
output_tar=~/Desktop/$crfilebasename.tar
output=~/Desktop/$crfilebasename.tgz

# tar czf $output ./reason.txt "./output/$(ls -t ./output | head -1)" "./images/$(ls -t ./images | head -1)" ./debug.log ./debug1.log
tar cf $output_tar reason.txt "output/$(ls -t ./output | head -1)" debug.log 

for i in -1 -2 -3 -4
do
    FILE="images/$(ls -t ./images | head $i | tail -1)"
    if [ -e "$FILE" ]; then
         tar uf $output_tar "$FILE"
     fi
done

FILE="debug1.log"
if [ -e "$FILE" ]; then
    tar uf $output_tar "$FILE"
fi

gzip $output_tar && mv $output_tar.gz $output

rm reason.txt

zenity --notification --text="Generated $output. Don't forget to send this archive to Neptus development team."&
exit
