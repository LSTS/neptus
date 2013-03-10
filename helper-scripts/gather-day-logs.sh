#!/bin/bash
#############################################################################
# Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia   #
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
# European Union Public Licence - EUPL v.1.1 Usage                          #
# Alternatively, this file may be used under the terms of the EUPL,         #
# Version 1.1 only (the "Licence"), appearing in the file LICENCE.md        #
# included in the packaging of this file. You may not use this  work        #
# except in compliance with the Licence. Unless required by  applicable     #
# law or agreed to in writing, software distributed under the Licence  is   #
# distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF      #
# ANY KIND, either express or implied. See the Licence for the specific     #
# language governing permissions and limitations at                         #
# https://www.lsts.pt/neptus/licence.                                       #
#                                                                           #
# For more information please see <http://lsts.fe.up.pt/neptus>.            #
#############################################################################
# Author: Paulo Dias, José Pinto                                            #
#############################################################################
# This script will prepare the logs from Neptus or collected by Neptus to   #
# be uploaded to the mission repository.                                    #
#############################################################################

PROGNAME=$0
PROGDIRNAME=`dirname $PROGNAME`


if [ -z "$BASH_VERSION" ]; then
    /bin/bash $0
    exit $?
fi

TEST_ZENITY=$(zenity -h 2>&1 1>/dev/null; echo $?)
if [ "$TEST_ZENITY" -ne 0 ]; then
    echo "Please install Zenity before proceding"
    exit $TEST_ZENITY
fi

TEST_7Z=$(7z 2>&1 1>/dev/null; echo $?)
if [ "$TEST_7Z" -ne 0 ]; then
    echo "Please install 7Zip before proceding"
    zenity --title="Please install 7Zip" --error --text="Please install 7Zip before proceding"
    exit $TEST_7Z
fi


START_PWD=$(pwd)
cd $PROGDIRNAME/..
NEPTUS_HOME=$(pwd)
cd $PROGDIRNAME
HOSTNAME=`hostname`
NEPTUSDIR=$(echo $HOSTNAME|tr "[:upper:]" "[:lower:]"|tr " " "_")

export todayFind=$(date +%Y%m%d)0000
export todayDirName=$(date +%Y%m%d)
export dest=to_upload_$todayDirName

STARTTIME=`date +%s`

if [ $(ps aux |grep -c  neptus.jar) -gt 1 ]; then
  echo "Seems that Neptus is open. Close it please..."
  zenity --title="Create package" --error --text="Seems that Neptus is open. Close it please..."
  exit 1
fi

PWD=$(pwd)
to_upload=`zenity --title="Select package destination folder" --file-selection --directory --save --filename=$NEPTUS_HOME/$dest`

if [ $? = 1 ]; then
  exit 1
fi


cd $NEPTUS_HOME

echo "Exporting from $NEPTUS_HOME/log to $to_upload"

(
touch -t $todayFind _start

echo "# Creating a clean folder '"$to_upload"/'"
rm -Rf $to_upload && mkdir $to_upload

if [ $? -ne 0 ]; then
  zenity --title="Create package" --error --text="Delete "$to_upload" please!";
  echo "Delete "$to_upload" please!";
  exit 1;
fi
echo 10

echo "# Moving 'log/downloaded' to '"$to_upload"/'"
echo "# Moving 'log/downloaded' to '"$to_upload"/'"
mv -v $NEPTUS_HOME/log/downloaded $to_upload/
echo "# Deleting LLF and temporary MRA files from 'log/downloaded'"
find $to_upload/downloaded -name *.llf|while read fx; do rm -v "$fx"; done;
find $to_upload/downloaded -name *.mra|while read fx; do rm -v "$fx"; done;
find $to_upload/downloaded -name lsf.index|while read fx; do rm -v "$fx"; done;

echo "# Compressing LSF files"
find $to_upload/downloaded -name *.lsf|while read fx; do gzip -n -9 -v "$fx"; done;

mv $to_upload/downloaded/* $to_upload/ && rmdir $to_upload/downloaded

echo 25
echo "# Preparing Neptus log dir"
mkdir -p $to_upload/$NEPTUSDIR/$todayDirName
mv -v $NEPTUS_HOME/log/* $to_upload/$NEPTUSDIR/$todayDirName
git rev-parse HEAD > $to_upload/$NEPTUSDIR/$todayDirName/scminfo.txt
git describe - -dirty >> $to_upload/$NEPTUSDIR/$todayDirName/scminfo.txt
git describe - -all - -long - -dirty >> $to_upload/$NEPTUSDIR/$todayDirName/scminfo.txt
git log -1 --date=iso >> $to_upload/$NEPTUSDIR/$todayDirName/scminfo.txt
git status --untracked-files=yes >> $to_upload/$NEPTUSDIR/$todayDirName/scminfo.txt
echo 40
echo "# Finding used  mission file..."
find $NEPTUS_HOME/missions/ -type f -name '*.nmisz' -newer _start -exec cp -v {} $to_upload/$NEPTUSDIR/$todayDirName \;
echo 50
cd $to_upload/$NEPTUSDIR/$todayDirName/
echo "# Zipping debug.log*"
zip -rv log-debug.zip debug.log* && rm -v debug.log*
echo 60
echo "# Zipping output/*"
zip -rv output.zip output/* && rm -rvf output/
echo 70

# echo "# Zipping messages/*"
# zip -rv messages.zip messages/* && rm -rvf messages/

echo "# Deleting LLF and temporary MRA files from 'log/messages'"
find messages -name *.llf|while read fx; do rm -v "$fx"; done;
find messages -name *.mra|while read fx; do rm -v "$fx"; done;
find messages -name lsf.index|while read fx; do rm -v "$fx"; done;
echo "# Compressing LSF files"
find messages -name *.lsf|while read fx; do gzip -n -9 -v "$fx"; done;

echo 80
echo "# 7zipping mission_state/*"
7z a -t7z mission_state.7z mission_state/* -mx5  && rm -rvf mission_state/
echo 90

cd $NEPTUS_HOME
rm -v _start
cd $START_PWD

ENDTIME=`date +%s`
TOTALTIME=$(($ENDTIME-$STARTTIME))
echo "#Work done in $TOTALTIME seconds."
echo 100
) | zenity --progress --title="Creating package" --text="Initializing" --pulsate

zenity --notification --text="Check $to_upload/$NEPTUSDIR/$todayDirName and see if something is missing or unneeded."
