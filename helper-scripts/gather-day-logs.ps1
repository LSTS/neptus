<#
.SYNOPSIS
    Prepares Neptus logs for upload (Windows/PowerShell version).

.DESCRIPTION
    This script gathers logs, compresses specific file types, collects git info,
    and organizes them into a timestamped directory for upload.
    It replaces the original 'gather-day-logs.sh' for Windows environments.

.NOTES
    Author: LSTS
    Original Copyright (c) 2004-2026 Universidade do Porto - LSTS

.RUN NOTES
    If running into policies issues run it with:
    - powershell -ExecutionPolicy Bypass -File .\helper-scripts\gather-day-logs.ps1
    Or activate policy for the user:
    - Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
#>

param(
    [string]$NeptusHomeOverride = ""
)

# Load required assemblies for GUI elements
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# --- Helper Functions ---

function Get-7ZipPath {
    if (Get-Command "7z" -ErrorAction SilentlyContinue) { return "7z" }
    $paths = @("C:\Program Files\7-Zip\7z.exe", "C:\Program Files (x86)\7-Zip\7z.exe")
    foreach ($p in $paths) { if (Test-Path $p) { return $p } }
    return $null
}

function Show-MessageBox {
    param([string]$Message, [string]$Title, [string]$Icon="None")
    [System.Windows.Forms.MessageBox]::Show($Message, $Title, [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]$Icon)
}

function Gzip-File {
    param([string]$FilePath)
    $7z = Get-7ZipPath
    if ($7z) {
        # Use 7zip to create gzip to match script behavior (max compression)
        & $7z a -tgzip -mx9 "$FilePath.gz" "$FilePath" | Out-Null
    } else {
        # .NET Fallback
        $srcFile = Get-Item -Path $FilePath
        $newFileName = "$($srcFile.FullName).gz"
        $srcStream = $srcFile.OpenRead()
        $targetStream = [System.IO.File]::Create($newFileName)
        $gzipStream = New-Object System.IO.Compression.GZipStream($targetStream, [System.IO.Compression.CompressionMode]::Compress)
        $srcStream.CopyTo($gzipStream)
        $gzipStream.Dispose()
        $targetStream.Dispose()
        $srcStream.Dispose()
    }
}

function Compress-LSFFiles {
    param([string]$Folder)
    Write-Host "# Compressing LSF files in $Folder"
    Get-ChildItem -Path $Folder -Include "*.lsf", "*IMC.xml" -Recurse | ForEach-Object {
        if (-not (Test-Path "$($_.FullName).gz")) {
            Gzip-File -FilePath $_.FullName
        }
    }
}

function Remove-MRALeftovers {
    param([string]$Folder)
    Write-Host "# Cleaning leftovers in $Folder"
    
    # 1. Standard patterns from original script
    $exts = @("*.llf", "*.mra", "lsf.index", "*.lsf", "IMC.xml")
    foreach ($ext in $exts) {
        Get-ChildItem -Path $Folder -Include $ext -Recurse -ErrorAction SilentlyContinue | 
            Remove-Item -Force -Recurse
    }

    # 2. Targeted "mra" folder removal
    # We find all directories named 'mra' and nuke them
    $mraFolders = Get-ChildItem -Path $Folder -Filter "mra" -Recurse -Directory -ErrorAction SilentlyContinue
    
    foreach ($dir in $mraFolders) {
        try {
            Write-Host "Removing non-empty directory: $($dir.FullName)" -ForegroundColor Cyan
            # Force removal of the directory and everything inside it
            Remove-Item -Path $dir.FullName -Recurse -Force -ErrorAction Stop
        } catch {
            # Backup: If standard Remove-Item fails (sometimes due to long paths or stubborn locks)
            Write-Warning "Standard delete failed for $($dir.Name), attempting forced filesystem delete..."
            [System.IO.Directory]::Delete($dir.FullName, $true)
        }
    }
}

# --- Main Script ---

# 1. Check for 7-Zip
$7z = Get-7ZipPath
if (-not $7z) {
    Show-MessageBox -Message "Please install 7-Zip before proceeding." -Title "Missing Dependency" -Icon "Error"
    exit 1
}

# 2. Setup Paths
$ScriptDir = $PSScriptRoot
if ($NeptusHomeOverride) {
    $NeptusHome = $NeptusHomeOverride
} else {
    $NeptusHome = Resolve-Path "$ScriptDir\.."
}
$Hostname = $env:COMPUTERNAME.ToLower().Replace(" ", "_")
$TodayDate = Get-Date -Format "yyyyMMdd"
$DestDefaultName = "to_upload_$TodayDate"

# 3. Check if Neptus is running
$NeptusRunning = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*neptus.jar*" }
if ($NeptusRunning) {
    Show-MessageBox -Message "Seems that Neptus is open. Close it please..." -Title "Create package" -Icon "Error"
    exit 1
}

# 4. Select Destination Folder
$FolderBrowser = New-Object System.Windows.Forms.FolderBrowserDialog
$FolderBrowser.Description = "Select package destination folder"
$FolderBrowser.SelectedPath = $NeptusHome
$DialogResult = $FolderBrowser.ShowDialog()

if ($DialogResult -ne "OK") { exit 1 }

$ToUploadRoot = Join-Path -Path $FolderBrowser.SelectedPath -ChildPath $DestDefaultName

if (Test-Path $ToUploadRoot) {
    Write-Host "Destination folder '$ToUploadRoot' already exists!!" -ForegroundColor Red
    exit 1
}

# Start Processing
$StartTime = Get-Date
$StartMarkerTime = $StartTime

try {
    Write-Progress -Activity "Gathering Logs" -Status "Creating clean folder" -PercentComplete 10
    New-Item -ItemType Directory -Path $ToUploadRoot | Out-Null
    
    # Move log/downloaded
    Write-Progress -Activity "Gathering Logs" -Status "Moving 'log/downloaded'" -PercentComplete 15
    $LogDownloaded = "$NeptusHome\log\downloaded"
    if (Test-Path $LogDownloaded) {
        Move-Item -Path $LogDownloaded -Destination "$ToUploadRoot\" -Force
        
        Compress-LSFFiles -Folder "$ToUploadRoot\downloaded"
        Remove-MRALeftovers -Folder "$ToUploadRoot\downloaded"
        
        # Move contents up one level and remove 'downloaded' folder (mimicking script behavior)
        Get-ChildItem "$ToUploadRoot\downloaded" | Move-Item -Destination $ToUploadRoot -Force
        Remove-Item "$ToUploadRoot\downloaded" -Force
    }

    Write-Progress -Activity "Gathering Logs" -Status "Preparing Neptus log dir" -PercentComplete 25
    $TargetLogDir = "$ToUploadRoot\$Hostname\$TodayDate"
    New-Item -ItemType Directory -Path $TargetLogDir -Force | Out-Null

    # SCM Info
    Write-Progress -Activity "Gathering Logs" -Status "Preparing SCM info" -PercentComplete 27
    $ScmFile = "$TargetLogDir\scminfo.txt"
    if (Get-Command "git" -ErrorAction SilentlyContinue) {
        Push-Location $NeptusHome
        if (git status 2>$null) {
            "HEAD: $(git rev-parse HEAD)" | Out-File $ScmFile
            git describe --dirty >> $ScmFile
            git describe --all --long --dirty >> $ScmFile
            git log -1 --date=iso >> $ScmFile
            git status --untracked-files=no >> $ScmFile
            "`r`n`r`n------ Git Diff ------" >> $ScmFile
            git diff --no-ext-diff >> $ScmFile
        }
        Pop-Location
    }

    # Move Neptus Logs
    Write-Progress -Activity "Gathering Logs" -Status "Moving Neptus logs" -PercentComplete 30
    Get-ChildItem "$NeptusHome\log\*" | Move-Item -Destination $TargetLogDir -Force

    # Mission Files
    Write-Progress -Activity "Gathering Logs" -Status "Finding used mission files" -PercentComplete 40
    Get-ChildItem "$NeptusHome\missions" -Recurse -Filter "*.nmisz" | Where-Object { $_.LastWriteTime -gt $StartMarkerTime } | Copy-Item -Destination $TargetLogDir -Force

    # Conf Files
    Write-Progress -Activity "Gathering Logs" -Status "Finding modified conf files" -PercentComplete 43
    $ConfDest = "$TargetLogDir\conf-files-modified"
    New-Item -ItemType Directory -Path $ConfDest -Force | Out-Null
    Get-ChildItem "$NeptusHome\conf" -Recurse -File | Where-Object { $_.LastWriteTime -gt $StartMarkerTime } | Copy-Item -Destination $ConfDest -Force

    # Zipping and Cleaning
    Write-Progress -Activity "Gathering Logs" -Status "Zipping and organizing" -PercentComplete 50
    Push-Location $TargetLogDir
    
    # Zip Conf files
    if (Test-Path "conf-files-modified") {
        Compress-Archive -Path "conf-files-modified" -DestinationPath "conf-files-modified.zip" -Force
        Remove-Item "conf-files-modified" -Recurse -Force
    }

    # Zip debug logs
    $DebugLogs = Get-ChildItem -Filter "*.log*"
    if ($DebugLogs) {
        Compress-Archive -Path $DebugLogs.FullName -DestinationPath "log-debug.zip" -Force
        $DebugLogs | Remove-Item -Force
    }
    
    # Zip Output
    if (Test-Path "output") {
        Compress-Archive -Path "output\*" -DestinationPath "output.zip" -Force
        Remove-Item "output" -Recurse -Force
    }
    
    # Process Messages
    if (Test-Path "messages") {
        Compress-LSFFiles -Folder "messages"
        Remove-MRALeftovers -Folder "messages"
    }

    # 7zip Mission State (Requires 7z)
    Write-Progress -Activity "Gathering Logs" -Status "7-Zipping mission_state" -PercentComplete 80
    if (Test-Path "mission_state") {
        & $7z a -t7z "mission_state.7z" "mission_state\*" -mx5 | Out-Null
        Remove-Item "mission_state" -Recurse -Force
    }

    Pop-Location

} catch {
    Show-MessageBox -Message "An error occurred: $($_.Exception.Message)" -Title "Error" -Icon "Error"
    exit 1
}

$EndTime = Get-Date
$TotalTime = ($EndTime - $StartTime).TotalSeconds
Write-Progress -Activity "Gathering Logs" -Status "Done" -Completed

$ResultText = "Work done in $([math]::Round($TotalTime)) seconds.`nCheck $TargetLogDir and see if something is missing or unneeded."
Show-MessageBox -Message $ResultText -Title "Process Complete" -Icon "Information"
