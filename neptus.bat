@echo off

if "%OS%"=="Windows_NT" @setlocal

set WORKSPACE=pt.up.fe.dceg.neptus.loader.NeptusMain ws
set VIEWER3D=pt.up.fe.dceg.neptus.loader.Viewer3DLoader
set MRA=pt.up.fe.dceg.neptus.loader.NeptusMain mra
set BUOY_CONSOLE=pt.up.fe.dceg.neptus.mc.consoletracker.BuoyConsole
set MODEMSHELL=pt.up.fe.dceg.middleware.amodem.AcousticModem %COMMPORT% 0 140
set IMTEXTCONSOLE=pt.up.fe.dceg.neptus.im.IMTextConsole
set LAUVCONSOLE=pt.up.fe.dceg.neptus.mc.lauvconsole.LAUVConsole
set TELECONSOLE=pt.up.fe.dceg.neptus.loader.TeleOperationConsoleLoader
set WORLDMAPPANEL=pt.up.fe.dceg.neptus.app.tiles.WorldMapPanel

set DEFAULT=pt.up.fe.dceg.neptus.loader.NeptusMain

set CLASSPATH=.;bin/neptus.jar;conf;./lib/RXTXcomm.jar;./lib/StarfireExt.jar;./lib/charsets-zip.jar;./lib/commons-cli-1.2.jar;./lib/commons-codec-1.7.jar;./lib/commons-compress-1.3.jar;./lib/commons-configuration-1.9.jar;./lib/commons-email-1.2.jar;./lib/commons-io-2.4.jar;./lib/commons-lang-2.6.jar;./lib/commons-lang3-3.1.jar;./lib/commons-logging-1.1.1.jar;./lib/commons-net-1.4.0.jar;./lib/dom4j/dom4j-1.6.1.jar;./lib/dom4j/jaxen-1.1.1.jar;./lib/fop/avalon-framework-4.2.0.jar;./lib/fop/batik.jar;./lib/fop/fop.jar;./lib/fop/jimi-1.0.jar;./lib/fop/xml-apis-ext.jar;./lib/fop/xmlgraphics-commons-1.3.1.jar;./lib/foxtrot.jar;./lib/gpsinput-0.5.3.jar;./lib/gson-2.2.2.jar;./lib/guava-13.0.1.jar;./lib/htmlUnit/cssparser-0.9.8.jar;./lib/htmlUnit/htmlunit-2.11.jar;./lib/htmlUnit/htmlunit-core-js-2.11.jar;./lib/htmlUnit/httpmime-4.2.2.jar;./lib/htmlUnit/nekohtml-1.9.17.jar;./lib/httpclient-4.2.2.jar;./lib/httpcore-4.2.2.jar;./lib/iText-2.1.5.jar;./lib/imgscalr-lib-4.2.jar;./lib/ini4j-0.5.2.jar;./lib/j3dcore.jar;./lib/j3dutils.jar;./lib/jchart2d-1.03.jar;./lib/jcommon-1.0.10.jar;./lib/jdic-linux/jdic_stub.jar;./lib/jdic-windows/jdic_stub.jar;./lib/jetty/jetty-6.0.2.jar;./lib/jetty/jetty-util-6.0.2.jar;./lib/jetty/servlet-api-2.5-6.0.2.jar;./lib/jfreechart-1.0.6.jar;./lib/jgoodies-common-1.4.0.jar;./lib/jgoodies-looks-2.5.2.jar;./lib/jh_image_filters.jar;./lib/jinput.jar;./lib/jmatio.jar;./lib/jme3/eventbus.jar;./lib/jme3/j-ogg-oggd.jar;./lib/jme3/j-ogg-vorbisd.jar;./lib/jme3/jME3-core.jar;./lib/jme3/jME3-desktop.jar;./lib/jme3/jME3-effects.jar;./lib/jme3/jME3-jbullet.jar;./lib/jme3/jME3-jogg.jar;./lib/jme3/jME3-lwjgl-natives.jar;./lib/jme3/jME3-lwjgl.jar;./lib/jme3/jME3-niftygui.jar;./lib/jme3/jME3-plugins.jar;./lib/jme3/jME3-terrain.jar;./lib/jme3/jbullet.jar;./lib/jme3/lwjgl.jar;./lib/jme3/nifty-default-controls.jar;./lib/jme3/nifty-style-black.jar;./lib/jme3/nifty.jar;./lib/jme3/stack-alloc.jar;./lib/jme3/xmlpull-xpp3.jar;./lib/jmf.jar;./lib/jogl2/gluegen-rt-natives-linux-amd64.jar;./lib/jogl2/gluegen-rt-natives-linux-i586.jar;./lib/jogl2/gluegen-rt-natives-macosx-universal.jar;./lib/jogl2/gluegen-rt-natives-windows-amd64.jar;./lib/jogl2/gluegen-rt-natives-windows-i586.jar;./lib/jogl2/gluegen-rt.jar;./lib/jogl2/gluegen.jar;./lib/jogl2/jogl-all-natives-linux-amd64.jar;./lib/jogl2/jogl-all-natives-linux-i586.jar;./lib/jogl2/jogl-all-natives-macosx-universal.jar;./lib/jogl2/jogl-all-natives-windows-amd64.jar;./lib/jogl2/jogl-all-natives-windows-i586.jar;./lib/jogl2/jogl-all.jar;./lib/jogl2/org.jzy3d-0.9.jar;./lib/jsch-0.1.40.jar;./lib/jxlayer.jar;./lib/l2fprod-common-all.jar;./lib/libimc.jar;./lib/log4j-1.2.17.jar;./lib/miglayout-4.0-swing.jar;./lib/percentlayout.jar;./lib/rhino.jar;./lib/soap/activation.jar;./lib/soap/mail.jar;./lib/speech/cmu_time_awb.jar;./lib/speech/cmu_us_kal.jar;./lib/speech/cmudict04.jar;./lib/speech/cmulex.jar;./lib/speech/cmutimelex.jar;./lib/speech/en_us.jar;./lib/speech/freetts.jar;./lib/speech/jsapi.jar;./lib/sqlitejdbc-v056.jar;./lib/standby.jar;./lib/swingx.jar;./lib/toolsUI-4.3.jar;./lib/vecmath.jar;./lib/vtk.jar;./lib/wms.jar;./lib/wrl/j3d-vrml97.jar;./lib/xalan-2.7.0/serializer.jar;./lib/xalan-2.7.0/xalan.jar;./lib/xalan-2.7.0/xsltc.jar;./lib/xerces-2.7.1/resolver.jar;./lib/xerces-2.7.1/xercesImpl.jar;./lib/xerces-2.7.1/xercesSamples.jar;./lib/xerces-2.7.1/xml-apis.jar;./lib/xj3d/FastInfoset.jar;./lib/xj3d/aviatrix3d-all.jar;./lib/xj3d/dis.jar;./lib/xj3d/disxml.jar;./lib/xj3d/geoapi.jar;./lib/xj3d/j3d-org.jar;./lib/xj3d/jutils.jar;./lib/xj3d/uri.jar;./lib/xj3d/vlc_uri.jar;./lib/xj3d/xj3d-all.jar;./lib/xj3d/xj3d-cefx3d.jar;./lib/xj3d/xj3d-common.jar;./lib/xj3d/xj3d-config.jar;./lib/xj3d/xj3d-core.jar;./lib/xj3d/xj3d-eai.jar;./lib/xj3d/xj3d-ecmascript.jar;./lib/xj3d/xj3d-external-sai.jar;./lib/xj3d/xj3d-images.jar;./lib/xj3d/xj3d-j3d.jar;./lib/xj3d/xj3d-java-sai.jar;./lib/xj3d/xj3d-jaxp.jar;./lib/xj3d/xj3d-jsai.jar;./lib/xj3d/xj3d-net.jar;./lib/xj3d/xj3d-norender.jar;./lib/xj3d/xj3d-ogl.jar;./lib/xj3d/xj3d-parser.jar;./lib/xj3d/xj3d-render.jar;./lib/xj3d/xj3d-runtime.jar;./lib/xj3d/xj3d-sai.jar;./lib/xj3d/xj3d-sav.jar;./lib/xj3d/xj3d-script-base.jar;./lib/xj3d/xj3d-xml-util.jar;./lib/xj3d/xj3d-xml.jar;./lib/xuggle-xuggler-5.4.jar;./lib/zxing-bin.jar

set LIBRARYPATH=.;libJNI

if exist jre\bin ( 
    set JAVA_BIN_FOLDER=jre\bin\
) else (
    set JAVA_BIN_FOLDER= 
)

for /f "delims=" %%a in ('%JAVA_BIN_FOLDER%java -cp bin/neptus.jar pt.up.fe.dceg.neptus.loader.helper.CheckJavaOSArch') do (@set JAVA_MACHINE_TYPE=%%a)
if %JAVA_MACHINE_TYPE%==x86 (
   set LIBRARYPATH=.;libJNI/x86;libJNI;C:\Program Files\VTK\vtk-5.8
) else (
   set LIBRARYPATH=.;libJNI/x64;libJNI;C:\Program Files (x86)\VTK\vtk-5.8
)



if not "%1"=="ws" goto end2
	set DEFAULT=%WORKSPACE%
	shift
:end2
if not "%1"=="v3d" goto end3
	set DEFAULT=%VIEWER3D%
	shift
:end3
if not "%1"=="mra" goto end4
	set DEFAULT=%MRA%
	shift
:end4
if not "%1"=="la" goto end8
	set DEFAULT=%LAUVCONSOLE%
	shift
:end8
if not "%1"=="wm" goto end10
	set DEFAULT=%WORLDMAPPANEL%
	shift
:end10

:endCheckApp

REM @echo on

set VMFLAGS="-XX:+HeapDumpOnOutOfMemoryError"

set OLDPATH=%PATH%
set PATH=%LIBRARYPATH%;%PATH%
%JAVA_BIN_FOLDER%java -Xms10m -Xmx912m -Dj3d.rend=d3d -Dsun.java2d.d3d=true %VMFLAGS% -Djava.library.path="%LIBRARYPATH%" -cp %CLASSPATH% %DEFAULT% %1 %2 %3 %4 %5 %6 %7 %8 %9
set PATH=%OLDPATH%
