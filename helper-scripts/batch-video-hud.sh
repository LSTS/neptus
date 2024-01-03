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
# Author: José Pinto                                                        #
#############################################################################
#                                                                           #
#############################################################################

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
