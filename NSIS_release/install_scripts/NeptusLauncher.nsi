#############################################################################
# Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia   #
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
# Author: José Pinto                                                        #
# Author: Paulo Dias                                                        #
#############################################################################
# This script is the NSIS script for neptus.exe launcher for Windows        #
#############################################################################

!include "x64.nsh"

# FUNCTIONS

Function GetExePath
	!define GetExePath `!insertmacro GetExePathCall`
 
	!macro GetExePathCall _RESULT
		Call GetExePath
		Pop ${_RESULT}
	!macroend
 
	Push $0
	Push $1
	Push $2
	StrCpy $0 $EXEDIR
	System::Call 'kernel32::GetLongPathNameA(t r0, t .r1, i 1024)i .r2'
	StrCmp $2 error +2
	StrCpy $0 $1
	Pop $2
	Pop $1
	Exch $0
FunctionEnd


 ; GetParameters
 ; input, none
 ; output, top of stack (replaces, with e.g. whatever)
 ; modifies no other variables.
 
Function GetParameters
 
  Push $R0
  Push $R1
  Push $R2
  Push $R3
  
  StrCpy $R2 1
  StrLen $R3 $CMDLINE
  
  ;Check for quote or space
  StrCpy $R0 $CMDLINE $R2
  StrCmp $R0 '"' 0 +3
    StrCpy $R1 '"'
    Goto loop
  StrCpy $R1 " "
  
  loop:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 $R1 get
    StrCmp $R2 $R3 get
    Goto loop
  
  get:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 " " get
    StrCpy $R0 $CMDLINE "" $R2
  
  Pop $R3
  Pop $R2
  Pop $R1
  Exch $R0
 
FunctionEnd

RequestExecutionLevel user

# Silent mode
SilentInstall silent

# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define COMPANY "LSTS - FEUP"
!define URL http://whale.fe.up.pt

# EXE Properties
Name "Neptus"
outFile "..\..\neptus.exe"
icon "..\static_files\icons\neptus.ico"

# Main Section
section
	# The exe path is now on var $exepath
	var /GLOBAL exepath1
	${GetExePath} $exepath1
	    
	Call GetParameters
	Pop $0
	
	SetOutPath $exepath1
	${If} ${RunningX64}
    ${DisableX64FSRedirection}
  ${EndIf}
  Exec "neptus.bat $0"
	${If} ${RunningX64}
    ${EnableX64FSRedirection}
  ${EndIf}
	
	;messageBox MB_OK "$exepath\neptus.bat $0"
	
sectionEnd
