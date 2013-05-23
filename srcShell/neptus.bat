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

set CLASSPATH=.;bin/neptus.jar;conf@NEPTUS_LIBS@

set LIBRARYPATH=.;libJNI

if exist jre\bin ( 
    set JAVA_BIN_FOLDER=jre\bin\
) else (
    set JAVA_BIN_FOLDER= 
)

for /f "delims=" %%a in ('%JAVA_BIN_FOLDER%java -cp bin/neptus.jar pt.up.fe.dceg.neptus.loader.helper.CheckJavaOSArch') do (@set JAVA_MACHINE_TYPE=%%a)
if %JAVA_MACHINE_TYPE%==x86 (
   set LIBRARYPATH=.;libJNI/x86;libJNI;C:\Program^ Files\VTK\bin
) else (
   set LIBRARYPATH=.;libJNI/x64;libJNI;C:\Program^ Files^ ^(x86^)\VTK\bin
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
