#!/bin/bash
#############################################################################
# Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática  #
# Departamento de Engenharia Electrotécnica e de Computadores               #
# Rua Dr. Roberto Frias, 4200-465 Porto, Portugal                           #
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
