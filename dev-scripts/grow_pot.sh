#!/bin/bash
#############################################################################
# Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia   #
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
# http://ec.europa.eu/idabc/eupl.html.                                      #
#                                                                           #
# For more information please see <http://lsts.fe.up.pt/neptus>.            #
#############################################################################
# Author: José Pinto, Paulo Dias                                            #
#############################################################################
# This script will generate the I18n POT file for internalization of Neptus #
#############################################################################

PROGNAME=$0
NEPTUS_HOME=`dirname $PROGNAME`/..
cd $NEPTUS_HOME

PROG="pt.lsts.neptus.i18n.PluginsPotGenerator"
      
ALL_LIBS=`find . -name *.jar | paste -sd ":" -`
CLASSPATH="./build/classes:./build/plugins":$ALL_LIBS

export LD_LIBRARY_PATH=".:libJNI"

if test -d jre/bin; then JAVA_BIN_FOLDER="jre/bin/"; else JAVA_BIN_FOLDER=""; fi

$JAVA_BIN_FOLDER"java" -Xms10m -Xmx1024m -Djava.library.path=".:libJNI" -cp $CLASSPATH $PROG

xgettext \
  --language=java \
  --keyword \
  --keyword=I18n.text \
  --keyword=I18n.textc:1,2c \
  --keyword=I18n.textf \
  --keyword=I18n.textfc:1,2c \
  --keyword=I18n.textmark \
  --keyword=I18n.textmarkc:1,2c \
  --keyword=PropertiesEditor.getPropertyInstance \
  --add-location \
  --from-code=UTF-8 \
  --add-comments=/ \
  --sort-output \
  --package-name=Neptus \
  --msgid-bugs-address=neptus.i18n@lsts-feup.org \
  --copyright-holder="2004-$(date +%Y) FEUP-LSTS" \
  -j \
  -o conf/i18n/neptus.pot \
  $(find . -name "*.java")
  


