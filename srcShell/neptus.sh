#!/bin/bash
PROGNAME=$0
NEPTUS_HOME=`dirname $PROGNAME`
cd $NEPTUS_HOME

WORKSPACE="pt.up.fe.dceg.neptus.loader.NeptusMain ws"
VIEWER3D="pt.up.fe.dceg.neptus.loader.Viewer3DLoader"
MRA="pt.up.fe.dceg.neptus.loader.NeptusMain mra"
LAUVCONSOLE="pt.up.fe.dceg.neptus.mc.lauvconsole.LAUVConsole"
WORLDMAPPANEL="pt.up.fe.dceg.neptus.app.tiles.WorldMapPanel"

DEFAULT="pt.up.fe.dceg.neptus.loader.NeptusMain"

CLASSPATH=".:bin/neptus.jar:conf@NEPTUS_LIBS@":$CLASSPATH

case $1 in
  "ws"       )  DEFAULT=$WORKSPACE
                shift;;
  "v3d"      )  DEFAULT=$VIEWER3D
                shift;;
  "mra"      )  DEFAULT=$MRA
                shift;;
  "la"       )  DEFAULT=$LAUVCONSOLE
                shift;;
  "wm"       )  DEFAULT=$WORLDMAPPANEL
                shift;;  
esac

LIBS=".:libJNI"

if test -d jre/bin; then JAVA_BIN_FOLDER="jre/bin/"; else JAVA_BIN_FOLDER=""; fi

JAVA_MACHINE_TYPE=$($JAVA_BIN_FOLDER"java" -cp bin/neptus.jar pt.up.fe.dceg.neptus.loader.helper.CheckJavaOSArch)
if [ ${JAVA_MACHINE_TYPE} == 'x64' ]; then
 LIBS=".:libJNI/x64:libJNI"
else
  LIBS=".:libJNI/x86:libJNI"
fi

export LD_LIBRARY_PATH=$LIBS
$JAVA_BIN_FOLDER"java" -Xms10m -Xmx1024m -Djava.library.path=$LIBS -cp $CLASSPATH $DEFAULT "$@"
