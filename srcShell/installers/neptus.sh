#!/bin/bash
PROGNAME=$0
NEPTUS_HOME=`dirname $PROGNAME`
cd $NEPTUS_HOME

CLASSPATH=".:bin/neptus.jar:conf@NEPTUS_LIBS@":$CLASSPATH

LIBS=".:libJNI"

if test -d jre/bin; then JAVA_BIN_FOLDER="jre/bin/"; else JAVA_BIN_FOLDER=""; fi

JAVA_MACHINE_TYPE=$($JAVA_BIN_FOLDER"java" -cp bin/neptus.jar pt.up.fe.dceg.neptus.loader.helper.CheckJavaOSArch)
if [ ${JAVA_MACHINE_TYPE} == 'x64' ]; then
 LIBS=".:libJNI/x64:libJNI"
else
  LIBS=".:libJNI/x86:libJNI"
fi

export LD_LIBRARY_PATH=".:libJNI"
$JAVA_BIN_FOLDER"java" -Xms10m -Xmx1024m -Djava.library.path=".:libJNI" -cp $CLASSPATH pt.up.fe.dceg.neptus.loader.NeptusMain "$@"
