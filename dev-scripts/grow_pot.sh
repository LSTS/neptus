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
# Author: José Pinto, Paulo Dias                                           #
#############################################################################
# This script will generate the I18n POT file for internalization of Neptus #
#############################################################################

PROGNAME=$0
NEPTUS_HOME=`dirname $PROGNAME`/..
cd $NEPTUS_HOME

PROG="pt.up.fe.dceg.neptus.i18n.PluginsPotGenerator"
      
CLASSPATH="./build/classes:./build/plugins:./lib/RXTXcomm.jar:./lib/StarfireExt.jar:./lib/charsets-zip.jar:./lib/commons-cli-1.2.jar:./lib/commons-codec-1.7.jar:./lib/commons-collections-3.2.1.jar:./lib/commons-compress-1.3.jar:./lib/commons-configuration-1.9.jar:./lib/commons-email-1.2.jar:./lib/commons-io-2.4.jar:./lib/commons-lang-2.6.jar:./lib/commons-lang3-3.1.jar:./lib/commons-logging-1.1.1.jar:./lib/commons-net-1.4.0.jar:./lib/dom4j/dom4j-1.6.1.jar:./lib/dom4j/jaxen-1.1.1.jar:./lib/fop/avalon-framework-4.2.0.jar:./lib/fop/batik.jar:./lib/fop/fop.jar:./lib/fop/jimi-1.0.jar:./lib/fop/xml-apis-ext.jar:./lib/fop/xmlgraphics-commons-1.3.1.jar:./lib/foxtrot.jar:./lib/gpsinput-0.5.3.jar:./lib/gson-2.2.2.jar:./lib/guava-13.0.1.jar:./lib/htmlUnit/cssparser-0.9.8.jar:./lib/htmlUnit/htmlunit-2.11.jar:./lib/htmlUnit/htmlunit-core-js-2.11.jar:./lib/htmlUnit/httpmime-4.2.2.jar:./lib/htmlUnit/nekohtml-1.9.17.jar:./lib/httpclient-4.2.2.jar:./lib/httpcore-4.2.2.jar:./lib/iText-2.1.5.jar:./lib/imgscalr-lib-4.2.jar:./lib/ini4j-0.5.2.jar:./lib/j3dcore.jar:./lib/j3dutils.jar:./lib/jakarta-oro-core-2.1-dev-1.jar:./lib/jakarta-oro-perl5-2.1-dev-1.jar:./lib/jchart2d-1.03.jar:./lib/jcommon-1.0.10.jar:./lib/jdic-linux/jdic_stub.jar:./lib/jdic-windows/jdic_stub.jar:./lib/jetty/jetty-6.0.2.jar:./lib/jetty/jetty-util-6.0.2.jar:./lib/jetty/servlet-api-2.5-6.0.2.jar:./lib/jfreechart-1.0.6.jar:./lib/jgoodies-common-1.4.0.jar:./lib/jgoodies-looks-2.5.2.jar:./lib/jh_image_filters.jar:./lib/jinput.jar:./lib/jmatio.jar:./lib/jmf.jar:./lib/jogl2/gluegen-rt-natives-linux-amd64.jar:./lib/jogl2/gluegen-rt-natives-linux-i586.jar:./lib/jogl2/gluegen-rt-natives-macosx-universal.jar:./lib/jogl2/gluegen-rt-natives-windows-amd64.jar:./lib/jogl2/gluegen-rt-natives-windows-i586.jar:./lib/jogl2/gluegen-rt.jar:./lib/jogl2/gluegen.jar:./lib/jogl2/jogl-all-natives-linux-amd64.jar:./lib/jogl2/jogl-all-natives-linux-i586.jar:./lib/jogl2/jogl-all-natives-macosx-universal.jar:./lib/jogl2/jogl-all-natives-windows-amd64.jar:./lib/jogl2/jogl-all-natives-windows-i586.jar:./lib/jogl2/jogl-all.jar:./lib/jogl2/org.jzy3d-0.9.jar:./lib/jsch-0.1.50.jar:./lib/jxlayer.jar:./lib/l2fprod-common-all.jar:./lib/libimc.jar:./lib/log4j-1.2.17.jar:./lib/miglayout-4.0-swing.jar:./lib/percentlayout.jar:./lib/rhino.jar:./lib/soap/activation.jar:./lib/soap/mail.jar:./lib/speech/cmu_time_awb.jar:./lib/speech/cmu_us_kal.jar:./lib/speech/cmudict04.jar:./lib/speech/cmulex.jar:./lib/speech/cmutimelex.jar:./lib/speech/en_us.jar:./lib/speech/freetts.jar:./lib/speech/jsapi.jar:./lib/sqlitejdbc-v056.jar:./lib/standby.jar:./lib/swingx.jar:./lib/toolsUI-4.3.jar:./lib/vecmath.jar:./lib/vtk.jar:./lib/wms.jar:./lib/wrl/j3d-vrml97.jar:./lib/xalan-2.7.0/serializer.jar:./lib/xalan-2.7.0/xalan.jar:./lib/xalan-2.7.0/xsltc.jar:./lib/xerces-2.7.1/resolver.jar:./lib/xerces-2.7.1/xercesImpl.jar:./lib/xerces-2.7.1/xercesSamples.jar:./lib/xerces-2.7.1/xml-apis.jar:./lib/xj3d/FastInfoset.jar:./lib/xj3d/aviatrix3d-all.jar:./lib/xj3d/dis.jar:./lib/xj3d/disxml.jar:./lib/xj3d/geoapi.jar:./lib/xj3d/j3d-org.jar:./lib/xj3d/jutils.jar:./lib/xj3d/uri.jar:./lib/xj3d/vlc_uri.jar:./lib/xj3d/xj3d-all.jar:./lib/xj3d/xj3d-cefx3d.jar:./lib/xj3d/xj3d-common.jar:./lib/xj3d/xj3d-config.jar:./lib/xj3d/xj3d-core.jar:./lib/xj3d/xj3d-eai.jar:./lib/xj3d/xj3d-ecmascript.jar:./lib/xj3d/xj3d-external-sai.jar:./lib/xj3d/xj3d-images.jar:./lib/xj3d/xj3d-j3d.jar:./lib/xj3d/xj3d-java-sai.jar:./lib/xj3d/xj3d-jaxp.jar:./lib/xj3d/xj3d-jsai.jar:./lib/xj3d/xj3d-net.jar:./lib/xj3d/xj3d-norender.jar:./lib/xj3d/xj3d-ogl.jar:./lib/xj3d/xj3d-parser.jar:./lib/xj3d/xj3d-render.jar:./lib/xj3d/xj3d-runtime.jar:./lib/xj3d/xj3d-sai.jar:./lib/xj3d/xj3d-sav.jar:./lib/xj3d/xj3d-script-base.jar:./lib/xj3d/xj3d-xml-util.jar:./lib/xj3d/xj3d-xml.jar:./lib/xuggle-xuggler-5.4.jar:./lib/zxing-bin.jar":dev-utils/junit-3.8.2.jar:$CLASSPATH

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
  


