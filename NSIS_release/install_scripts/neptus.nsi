#############################################################################
# Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia   #
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
# Author: Paulo Dias,José Pinto                                             #
#############################################################################
# This script is the NSIS script for Neptus installer for Windows           #
#############################################################################

Unicode True

!include neptus_include.nsi

; RequestExecutionLevel user
RequestExecutionLevel admin

; Needed to use LZMA compression in solid mode for better compression
SetCompressor /SOLID lzma
SetCompressorDictSize 32

# Defines
!define /date MyTIMESTAMP "%Y-%m-%d" 
!define REGKEY "SOFTWARE\$(^Name)"
;!define VERSION "NEPTUS ${MyTIMESTAMP}"
!define COMPANY "LSTS - FEUP"
!define URL https://lsts.fe.up.pt/toolchain/neptus

# MUI defines
!define MUI_ICON "..\static_files\icons\neptus.ico"
!define MUI_LICENSEPAGE
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_REGISTRY_KEY Software\$(^Name)${TEXTVERSION}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER $(^Name)${TEXTVERSION}
!define MUI_UNICON "..\static_files\icons\neptus-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "..\static_files\icons\neptus_header.bmp"
!define MUI_ABORTWARNING

LicenseText "$(^Name)${TEXTVERSION} Software License"
LicenseData "${BASEDIR}\LICENSE.md"

# Included files
!include Sections.nsh
!include fileassoc.nsh
!include MUI.nsh
!include util_ne.nsh
!include "x64.nsh"


# Language Selection Dialog Settings
;Remember the installer language
!define MUI_LANGDLL_REGISTRY_ROOT "HKCU" 
!define MUI_LANGDLL_REGISTRY_KEY "${REGKEY}" 
!define MUI_LANGDLL_REGISTRY_VALUENAME "Installer Language"


# Reserved Files
; If you are using solid compression, files that are required before
; the actual installation should be stored first in the data block,
; because this will make your installer start faster.    
!insertmacro MUI_RESERVEFILE_LANGDLL

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${BASEDIR}\LICENSE.md"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
;!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
; Show all languages, despite user's codepage
!define MUI_LANGDLL_ALLLANGUAGES
; Languages  
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "Portuguese"
; !insertmacro MUI_LANGUAGE "Russian"


# Installer attributes
Name "${NEPTUS_NAME}"

OutFile "${OUT_FOLDER}\${DIST_EXE}.exe"

; See onInit and un.onInit for when activate 64bit install on programfiles
;InstallDir $PROGRAMFILES\$(^Name)
InstallDir C:\$(^Name)

BrandingText "LSTS-FEUP and Authors. All rights reserved."
CRCCheck on
XPStyle on
#ShowInstDetails show
VIProductVersion ${VERSION}
VIAddVersionKey ProductName "$(^Name)${TEXTVERSION}"
VIAddVersionKey ProductVersion "${TEXTVERSION} (compiled on ${DATECOMPILED})"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion ""
VIAddVersionKey FileDescription ""
VIAddVersionKey LegalCopyright "${LEGALCOPY}"
InstallDirRegKey HKLM "${REGKEY}" Path
#ShowUninstDetails show

ComponentText "$(^Name)"

# Installer sections
Section "-$(^Name) base" sec1
    SetOutPath $INSTDIR
    SetOverwrite on        
    File ..\static_files\icons\neptus.ico
    File /r /x .svn ${BASEDIR}\*
    ;File  /oname=neptus.bat ..\static_files\batch_files\neptusse.bat
    ;File  /oname=neptus.bat  ${NEPTUS_BATCH_PATH}

    ${If} ${RunningX64}
       ; MessageBox MB_OK "running on x64"
       ; If x64 then remove jre 32 bits and rename the 64 bits one to jre
       IfFileExists $INSTDIR\jre64\*.* 0 +3
          RMDir /r $INSTDIR\jre
          Rename $INSTDIR\jre64 $INSTDIR\jre
    ${Else}
       ; MessageBox MB_OK "running on x86"
       ; If x32 then remove jre 64 bits
       RMDir /r $INSTDIR\jre64
    ${EndIf}

    SetOutPath $INSTDIR\icons
    File ..\static_files\icons\*.ico

    SetOutPath "$SMPROGRAMS\$StartMenuGroup"
    SetOutPath $INSTDIR

    ;SetShellVarContex all
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\$(^Name).lnk" '"$INSTDIR\neptus.bat"' '' '$INSTDIR\neptus.ico'
    
    ; To write the general-properties.xml with the language preference
    ${Switch} $LANGUAGE
      ${Case} 2070 ;Portuguese (2070)
      ${Case} 1046 ;Brazilian Portuguese (1046)
        ${WriteLangPref} "pt_PT"
      ${Break}
      ;${Case} 1049 ;Russian (1049)
      ;  ${WriteLangPref} "ru_RU"
      ;${Break}
    ${EndSwitch}

SectionEnd

Section /o "Desktop Shortcut" desktop
    SectionSetText ${desktop} "Create a Neptus launcher at your desktop"
    SetOutPath $Desktop
    SetOutPath $INSTDIR
    
    ;SetShellVarContex all
    CreateShortCut "$Desktop\$(^Name).lnk" '"$INSTDIR\neptus.bat"' '' '$INSTDIR\neptus.ico'
SectionEnd

Section "NMISZ File Association (.nmisz)" nmisassoc
    !insertmacro APP_ASSOCIATE "nmisz" "neptus.missionfile" "Neptus Mission" "$INSTDIR\icons\missionz.ico" \
    "Open Neptus Mission" "$INSTDIR\neptus.exe $\"%1$\""	   	   
SectionEnd

Section "-refreshAssociations" refreshAssociations
    System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) v (0x08000000, 0, 0, 0)'
SectionEnd

Section "-uninst" uninst
    WriteRegStr HKLM "${REGKEY}\Components" Main 1

    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup

    ;SetShellVarContex all
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk" $INSTDIR\uninstall.exe

    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    Goto done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o un.Main UNSEC0000
    RMDir /r /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RMDir /r /REBOOTOK $INSTDIR        
    Delete /REBOOTOK "$Desktop\$(^Name).lnk"
    DeleteRegValue HKLM "${REGKEY}\Components" Main
SectionEnd

Section un.post UNSEC0001
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /ifempty HKLM "${REGKEY}\Components"
    DeleteRegKey /ifempty HKLM "${REGKEY}"

    ;SetShellVarContex all
    RMDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RMDir /REBOOTOK $INSTDIR
SectionEnd

# Installer functions
Function .onInit
    ${If} ${RunningX64}
     ; SetRegView 64
     ; ${EnableX64FSRedirection}
     ; StrCpy $INSTDIR "$PROGRAMFILES64\$(^Name)"
     ; ; StrCpy $Caption_title_Name "$(^Name) x64"
    ${Else}
     ; StrCpy $INSTDIR "$PROGRAMFILES\$(^Name)"
     ; ; StrCpy $Caption_title_Name "$(^Name) x86"
    ${EndIf}

    !insertmacro MUI_LANGDLL_DISPLAY
    InitPluginsDir
FunctionEnd

# Uninstaller functions
Function un.onInit
    ${If} ${RunningX64}
        ; SetRegView 64
    ${EndIf}

    !insertmacro MUI_UNGETLANGUAGE
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    ReadRegStr $StartMenuGroup HKLM "${REGKEY}" StartMenuGroup
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
FunctionEnd
