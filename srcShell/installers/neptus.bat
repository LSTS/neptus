@echo off
rem #############################################################################
rem # Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia   #
rem # Laboratório de Sistemas e Tecnologia Subaquática (LSTS)                   #
rem # All rights reserved.                                                      #
rem # Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal            #
rem #                                                                           #
rem # This file is part of Neptus, Command and Control Framework.               #
rem #                                                                           #
rem # Commercial Licence Usage                                                  #
rem # Licencees holding valid commercial Neptus licences may use this file      #
rem # in accordance with the commercial licence agreement provided with the     #
rem # Software or, alternatively, in accordance with the terms contained in a   #
rem # written agreement between you and Universidade do Porto. For licensing    #
rem # terms, conditions, and further information contact lsts@fe.up.pt.         #
rem #                                                                           #
rem # Modified European Union Public Licence - EUPL v.1.1 Usage                 #
rem # Alternatively, this file may be used under the terms of the Modified EUPL,#
rem # Version 1.1 only (the "Licence"), appearing in the file LICENCE.md        #
rem # included in the packaging of this file. You may not use this  work        #
rem # except in compliance with the Licence. Unless required by  applicable     #
rem # law or agreed to in writing, software distributed under the Licence  is   #
rem # distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF      #
rem # ANY KIND, either express or implied. See the Licence for the specific     #
rem # language governing permissions and limitations at                         #
rem # https://github.com/LSTS/neptus/blob/develop/LICENSE.md                    #
rem # and http://ec.europa.eu/idabc/eupl.html.                                  #
rem #                                                                           #
rem # For more information please see <http://lsts.fe.up.pt/neptus>.            #
rem #############################################################################
rem # Author: Paulo Dias, José Pinto                                            #
rem #############################################################################

set CLASSPATH=.;bin/neptus.jar;conf@NEPTUS_LIBS@
set LIBRARYPATH=.;libJNI

if exist jre\bin ( 
    set JAVA_BIN_FOLDER=jre\bin\
) else (
    set JAVA_BIN_FOLDER= 
)

for /f "delims=" %%a in ('%JAVA_BIN_FOLDER%java -cp bin/neptus.jar pt.lsts.neptus.loader.helper.CheckJavaOSArch') do (@set JAVA_MACHINE_TYPE=%%a)

if %JAVA_MACHINE_TYPE%==windows-x86 (
  if %PROCESSOR_ARCHITECTURE%==x86 (
    if defined vtk.lib.dir (
      set VTKLIB=%vtk.lib.dir%
    ) else (
       set VTKLIB=%PROGRAMFILES%\VTK\bin
    )
  )
  else (
    if defined "vtk.lib.dir(x86)" (
      set VTKLIB=%vtk.lib.dir(x86)%
    ) else (
      if defined vtk.lib.dir (
        set VTKLIB=%vtk.lib.dir%
      ) else (
        set "VTKLIB=%PROGRAMFILES(x86)%\VTK\bin"
      )
    )
  )
) else (
  if defined vtk.lib.dir (
      set VTKLIB=%vtk.lib.dir%
  ) else (
    set VTKLIB=%PROGRAMFILES%\VTK\bin
  )
)

if %JAVA_MACHINE_TYPE%==windows-x86 (
  if %PROCESSOR_ARCHITECTURE%==x86 (
    if defined opencv.lib.dir (
      set OPENCVLIB=%opencv.lib.dir%
    ) else (
       set OPENCVLIB=%PROGRAMFILES%\opencv\bin
    )
  )
  else (
    if defined "opencv.lib.dir(x86)" (
      set OPENCVLIB=%opencv.lib.dir(x86)%
    ) else (
      if defined opencv.lib.dir (
        set OPENCVLIB=%opencv.lib.dir%
      ) else (
        set "OPENCVLIB=%PROGRAMFILES(x86)%\opencv\bin"
      )
    )
  )
) else (
  if defined opencv.lib.dir (
      set OPENCVLIB=%opencv.lib.dir%
  ) else (
    set OPENCVLIB=%PROGRAMFILES%\opencv\bin
  )
)

if %JAVA_MACHINE_TYPE%==windows-x86 (
  set LIBRARYPATH=.;libJNI\x86;libJNI\gdal\win\x86;libJNI;%VTKLIB%;%OPENCVLIB%
) else (
  set LIBRARYPATH=.;libJNI\x64;libJNI\gdal\win\x64;libJNI;%VTKLIB%;%OPENCVLIB%
)

set VMFLAGS="-XX:+HeapDumpOnOutOfMemoryError"

set OLDPATH=%PATH%
set PATH=%LIBRARYPATH%;%PATH%
start %JAVA_BIN_FOLDER%javaw.exe -Xms10m -Xmx912m -Dj3d.rend=d3d -Dsun.java2d.d3d=true %VMFLAGS% -Djava.library.path="%LIBRARYPATH%" -cp %CLASSPATH% pt.lsts.neptus.loader.NeptusMain %1 %2 %3 %4 %5 %6 %7 %8 %9
set PATH=%OLDPATH%
