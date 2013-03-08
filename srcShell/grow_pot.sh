#!/bin/bash
#############################################################################
# Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática  #
# Departamento de Engenharia Electrotécnica e de Computadores               #
# Rua Dr. Roberto Frias, 4200-465 Porto, Portugal                           #
#############################################################################
# Author: José Pinto, Paulo Dias                                           #
#############################################################################
# $Id:: grow_pot.sh 9652 2013-01-03 15:46:09Z pdias                       $:#
#############################################################################
# This script will generate the I18n POT file for internalization of Neptus #
#############################################################################

PROGNAME=$0
NEPTUS_HOME=`dirname $PROGNAME`/..
cd $NEPTUS_HOME

PROG="pt.up.fe.dceg.neptus.i18n.PluginsPotGenerator"
      
CLASSPATH="./build/classes:./build/plugins@NEPTUS_LIBS@":dev-utils/junit-3.8.2.jar:$CLASSPATH

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
  


