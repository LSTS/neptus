
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

JAVA_OPTS="-Xmx4096m" ../neptus run pt.lsts.neptus.plugins.mraplots.ScriptLog $@;


