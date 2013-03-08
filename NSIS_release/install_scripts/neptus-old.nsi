!include neptus_include.nsi

# Defines
!define /date MyTIMESTAMP "%Y-%m-%d" 
!define REGKEY "SOFTWARE\$(^Name)"
;!define VERSION "NEPTUS ${MyTIMESTAMP}"
!define COMPANY "LSTS - FEUP"
!define URL http://whale.fe.up.pt/neptus

# MUI defines
!define MUI_ICON "..\static_files\icons\neptus.ico"
!define MUI_LICENSEPAGE
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_REGISTRY_KEY Software\Neptus
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER Neptus
!define MUI_UNICON "..\static_files\icons\neptus-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "..\static_files\icons\neptus_header.bmp"
!define MUI_ABORTWARNING

LicenseText "Neptus Software License"
LicenseData "${BASEDIR}\legal\Neptus-LICENSE.txt"

# Included files
!include Sections.nsh
!include fileassoc.nsh
!include MUI.nsh

# Reserved Files

# Variables
Var StartMenuGroup

# Installer pages
#!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${BASEDIR}\legal\Neptus-LICENSE.txt"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
#!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English



# Installer attributes
Name "Neptus"
#OutFile "..\..\Neptus-${NAMEDRELEASE}-x86-32bit-${MyTIMESTAMP}.exe"
OutFile "..\..\${DIST_EXE}.exe"
#OutFile Neptus.exe
InstallDir $PROGRAMFILES\Neptus
BrandingText "LSTS-FEUP and Authors. All rights reserved."
CRCCheck on
XPStyle on
#ShowInstDetails show
VIProductVersion ${VERSION}
VIAddVersionKey ProductName "Neptus"
VIAddVersionKey ProductVersion "${TEXTVERSION} ${NAMEDRELEASE} (compiled by ${COMPILEDBY} at ${DATECOMPILED})"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion ""
VIAddVersionKey FileDescription ""
VIAddVersionKey LegalCopyright "${LEGALCOPY}"
InstallDirRegKey HKLM "${REGKEY}" Path
#ShowUninstDetails show

ComponentText "Neptus Framework"

# Installer sections
Section "-Neptus base" sec1

    SetOutPath $INSTDIR
    SetOverwrite on    
    File ..\static_files\batch_files\neptus.bat
;    File ..\..\neptus.exe
;    File ..\..\neptus-config.xsd
;    File ..\..\neptus-config.xml
;    File ..\..\libJNI\ortemanager.exe
    File ..\static_files\icons\neptus.ico
    File /r /x .svn ${BASEDIR}\*

    SetOutPath $INSTDIR\icons
	File ..\static_files\icons\*.ico

;    SetOutPath $INSTDIR\conf
;    File /r /x .svn ..\..\conf\*

;    SetOutPath $INSTDIR\files
;    File /r /x .svn ..\..\files\*

;    SetOutPath $INSTDIR\images
;    File /r /x .svn ..\..\images\* 

;    SetOutPath $INSTDIR\jre
;    ;File /r /x .svn /x *.txt ..\jre\*
;    File /r /x .svn /x *.txt ..\..\dist\jre-windows\*

;    ;SetOutPath $INSTDIR\jre\bin
;    ;File /r /x .svn ..\libJNI

;    SetOutPath $INSTDIR\jre\lib\ext
;    File /x .svn ..\..\lib\*
;    File /x .svn ..\..\lib\axis\*
;    File /x .svn ..\..\lib\dom4j\*
;    File /x .svn ..\..\lib\fop\*
;    File /x .svn ..\..\lib\ptplot\*
;    File /x .svn ..\..\lib\soap\*
;    File /x .svn ..\..\lib\xalan-2.7.0\*
;    File /x .svn ..\..\lib\xerces-2.7.1\*
;    File /x .svn ..\..\lib\xj3d\*
;    File ..\..\bin\neptus.jar    
;    File ..\..\libJNI\jjstick.dll


;    SetOutPath $INSTDIR\jre\bin
;    File /r /x .svn ..\..\libJNI\*.dll
;    File /r /x .svn ..\..\libJNI\win\*.dll
;    File /r /x .svn ..\..\libJNI\*.exe

;    ;SetOutPath $INSTDIR\libJNI
;    ;File /r /x .svn ..\..\libJNI\*

;    ;SetOutPath $INSTDIR\schemas
;    ;File /r /x .svn ..\..\schemas\*
    
;    SetOutPath $INSTDIR\vehicles-defs
;    File /r /x .svn ..\..\vehicles-defs\*
        
;    SetOutPath $INSTDIR\vehicles-files
;    File /r /x .svn ..\..\vehicles-files\*


;    SetOutPath $INSTDIR\legal
;    File /r /x .svn ..\..\legal\*

;    SetOutPath $INSTDIR\log
;    #File /r /x .svn /x *.txt /x *.log /x *.bin ..\..\log\*
	
    SetOutPath "$SMPROGRAMS\$StartMenuGroup"
    SetOutPath $INSTDIR
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Neptus.lnk" '"$INSTDIR\neptus.bat"' '' '$INSTDIR\neptus.ico'
SectionEnd

SectionGroup "Application Launchers"
;	Section "MonSense" monsense
;	    SetOutPath $INSTDIR
;	    File ..\static_files\icons\monsense.ico
;	    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\MonSense.lnk" '"$INSTDIR\neptus.bat"' 'monsense' '$INSTDIR\monsense.ico'
;	SectionEnd
	Section "Mission Review & Analisys" mra
	    SetOutPath $INSTDIR
	    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Mission Review & Analisys.lnk" '"$INSTDIR\neptus.bat"' 'mra' '$INSTDIR\neptus.ico'
	SectionEnd
;	Section "Mission Planner" mp
;	    SetOutPath $INSTDIR
;	    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\Mission Planner.lnk" '"$INSTDIR\neptus.bat"' 'mp' '$INSTDIR\neptus.ico'
;	SectionEnd
SectionGroupEnd

Section /o "Desktop Shortcut" desktop
	SectionSetText desktop "Create a Neptus launcher at your desktop"
	SetOutPath $Desktop
	SetOutPath $INSTDIR
	CreateShortCut "$Desktop\Neptus.lnk" '"$INSTDIR\neptus.bat"' '' '$INSTDIR\neptus.ico'
SectionEnd

;Section /o "Neptus plugins" plugins
;	SectionSetText plugins "Install Neptus Plugins"
;	SetOutPath $INSTDIR\plugins
;	File /r /x .svn ..\..\plugins\* 
;SectionEnd

SectionGroup "File Associations" fileassoc
	Section "Neptus Missions (.nmis)" nmisassoc
	    !insertmacro APP_ASSOCIATE "nmis" "neptus.missionfile" "Neptus Mission" "$INSTDIR\icons\mission.ico" \
	    "Open Neptus Mission" "$INSTDIR\neptus.exe -f $\"%1$\""
	SectionEnd
        
	Section "NMISZ File Association (.nmisz)" nmiszassoc
            !insertmacro APP_ASSOCIATE "nmisz" "neptus.missionfile" "Neptus Mission" "$INSTDIR\icons\missionz.ico" \
	    "Open Neptus Mission Z" "$INSTDIR\neptus.exe $\"%1$\""
	SectionEnd
	
	Section "Neptus Checklists (.nchk)" nchkassoc
	    !insertmacro APP_ASSOCIATE "nchk" "neptus.checklist" "Neptus Checklist" "$INSTDIR\icons\checklist.ico" \
	    "Open Neptus Checklist" "$INSTDIR\neptus.exe -f $\"%1$\""
	SectionEnd

;	Section "Neptus Maps (.nmap)" nmapassoc
;	    !insertmacro APP_ASSOCIATE "nmap" "neptus.mapfile" "Neptus Map" "$INSTDIR\icons\map.ico" \
;	    "Open Neptus Map" "$INSTDIR\neptus.exe -f $\"%1$\""
;	SectionEnd
	
;	Section "Neptus Consoles (.ncon)" nconassoc
;	    !insertmacro APP_ASSOCIATE "ncon" "neptus.console" "Neptus Console" "$INSTDIR\icons\console.ico" \
;	    "Open Neptus Console" "$INSTDIR\neptus.exe -f $\"%1$\""
;	SectionEnd
	
;	Section "Neptus Configurations (.ncfg)" ncfgassoc
;	    !insertmacro APP_ASSOCIATE "ncfg" "neptus.configuration" "Neptus Configuration" "$INSTDIR\icons\config.ico" \
;	    "Open Neptus Configuration" "$INSTDIR\neptus.exe -f $\"%1$\""
;	SectionEnd	
		
;	Section "Neptus Vehicles (.nvcl)" nvclassoc
;	    !insertmacro APP_ASSOCIATE "nvcl" "neptus.vehicle" "Neptus Vehicle" "$INSTDIR\icons\vehicle.ico" \
;	    "Open Neptus Vehicle" "$INSTDIR\neptus.exe -f $\"%1$\""
;	SectionEnd
	
;	Section "Neptus WSNs (.nwsn)" nwsnassoc
;	    !insertmacro APP_ASSOCIATE "nwsn" "neptus.wsn" "Neptus WSN" "$INSTDIR\icons\nwsn.ico" \
;	    "Open Neptus WSN" "$INSTDIR\neptus.exe -f $\"%1$\""
;	SectionEnd
	
;	Section "RMF files (.rmf)" rmfassoc
;	    !insertmacro APP_ASSOCIATE "rmf" "neptus.rmf" "RMF file" "$INSTDIR\icons\rmf.ico" \
;	    "Open RMF file" "$INSTDIR\neptus.exe -f $\"%1$\""	   	   
;	SectionEnd
	
SectionGroupEnd

Section "-refreshAssociations" refreshAssociations
	System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) v (0x08000000, 0, 0, 0)'
SectionEnd

;Section "Example Missions" missions
;    SetOutPath $INSTDIR\maps
;    File /r /x .svn ..\..\maps\*

;    SetOutPath $INSTDIR\missions
;    File /r /x .svn ..\..\missions\*
;SectionEnd
	
Section "-uninst" uninst
    WriteRegStr HKLM "${REGKEY}\Components" Main 1

    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
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
    Delete /REBOOTOK "$Desktop\Neptus.lnk"
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
    RMDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RMDir /REBOOTOK $INSTDIR
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    ReadRegStr $StartMenuGroup HKLM "${REGKEY}" StartMenuGroup
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
FunctionEnd

