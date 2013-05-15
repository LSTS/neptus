@echo off

set CLASSPATH=.;bin/neptus.jar;conf@NEPTUS_LIBS@
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
   set LIBRARYPATH=.;libJNI/x64;libJNI;C:\Program Files\VTK\vtk-5.8
)

set VMFLAGS="-XX:+HeapDumpOnOutOfMemoryError"

set OLDPATH=%PATH%
set PATH=%LIBRARYPATH%;%PATH%
start %JAVA_BIN_FOLDER%javaw.exe -Xms10m -Xmx912m -Dj3d.rend=d3d -Dsun.java2d.d3d=true %VMFLAGS% -Djava.library.path="%LIBRARYPATH%" -cp %CLASSPATH% pt.up.fe.dceg.neptus.mc.lauvconsole.LAUVConsole %1 %2 %3 %4 %5 %6 %7 %8 %9
set PATH=%OLDPATH%
