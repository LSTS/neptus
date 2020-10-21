#!/bin/bash

if [ -z "$BASH" ]
then
  bash $0 $@
fi

PROGNAME=$0

function command_exists {
  type "$1" &> /dev/null
}

unameOut="$(uname -s)"
case "${unameOut}" in
    Darwin*)    SHELL_DIR=`dirname $PROGNAME`;
                echo "MacOS found!";;
    *)          if command_exists readlink; then
                  SHELL_DIR=`dirname $(readlink -f $PROGNAME)`
                  echo "Readlink found!"
                else
                  SHELL_DIR=`dirname $PROGNAME`
                  echo "No readlink found!"
                fi
esac

cd "$SHELL_DIR/" > /dev/null
APP_HOME="`pwd -P`"
cd -

for f in `find $@ -name *.mjpg | sort -u`; do 
  folder=$(dirname $f);
  echo "Processing $folder.";
  JAVA_OPTS="-Xmx4096m" ../neptus run \
	  pt.lsts.neptus.mra.exporters.BatchMraExporter\
	  pt.lsts.neptus.plugins.mjpeg.VideoHudExporter $folder;
done
