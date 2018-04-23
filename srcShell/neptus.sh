#!/bin/bash
#############################################################################
# Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia   #
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
# https://github.com/LSTS/neptus/blob/develop/LICENSE.md                    #
# and http://ec.europa.eu/idabc/eupl.html.                                  #
#                                                                           #
# For more information please see <http://lsts.fe.up.pt/neptus>.            #
#############################################################################
# Author: Paulo Dias, José Pinto                                            #
#############################################################################

PROGNAME=$0
NEPTUS_HOME=`dirname $(readlink -f $PROGNAME)`
cd $NEPTUS_HOME

WORKSPACE="pt.lsts.neptus.loader.NeptusMain ws"
VIEWER3D="pt.lsts.neptus.loader.Viewer3DLoader"
MRA="pt.lsts.neptus.loader.NeptusMain mra"
LAUVCONSOLE="pt.lsts.neptus.mc.lauvconsole.LAUVConsole"
WORLDMAPPANEL="pt.lsts.neptus.app.tiles.WorldMapPanel"

DEFAULT="pt.lsts.neptus.loader.NeptusMain"

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

JAVA_MACHINE_TYPE=$($JAVA_BIN_FOLDER"java" -cp bin/neptus.jar pt.lsts.neptus.loader.helper.CheckJavaOSArch)
echo "Found machine type: $JAVA_MACHINE_TYPE"
if [ ${JAVA_MACHINE_TYPE} == 'linux-x64' ]; then
 LIBS=".:libJNI/x64:libJNI:/usr/lib/jni:libJNI/gdal/linux/x64:libJNI/europa/x64"
elif [ ${JAVA_MACHINE_TYPE} == 'linux-x86' ]; then
  LIBS=".:libJNI/x86:libJNI:/usr/lib/jni:libJNI/gdal/linux/x86"
elif [ ${JAVA_MACHINE_TYPE} == 'osx-x64' ]; then
  LIBS=".:libJNI/osx:libJNI:/usr/lib/jni"
fi

if test -f /usr/lib/jni/libvtkCommonJava.so; then
  VTKPROP="-Dvtk.lib.dir=/usr/lib/jni"
elif test -f /usr/lib/vtk-5.10/libvtkCommonJava.so; then
  VTKPROP="-Dvtk.lib.dir=/usr/lib/vtk-5.10"
elif test -f /usr/lib/vtk-5.8/libvtkCommonJava.so; then
  VTKPROP="-Dvtk.lib.dir=/usr/lib/vtk-5.8"
else
  VTKPROP=
  echo "No VTK Java wrappers found"
fi

export VMFLAGS="-XX:+HeapDumpOnOutOfMemoryError"

export LD_LIBRARY_PATH=$LIBS:$LD_LIBRARY_PATH
$JAVA_BIN_FOLDER"java" -Xms10m -Xmx1024m $VMFLAGS -Djava.library.path=$LIBS $VTKPROP -cp $CLASSPATH $DEFAULT "$@"
