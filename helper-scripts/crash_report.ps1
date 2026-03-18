<#
.SYNOPSIS
    Generates a crash report archive (Windows/PowerShell version).

.DESCRIPTION
    Creates a .tgz file containing user description, latest output, logs, 
    and recent images for debugging purposes.

.NOTES
    Original Author: José Pinto, Paulo Dias

.RUN NOTES
    If running into policies issues run it with:
    - powershell -ExecutionPolicy Bypass -File .\helper-scripts\crash_report.ps1
    Or activate policy for the user:
    - Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
#>

Add-Type -AssemblyName Microsoft.VisualBasic

# --- Paths ---
$ScriptDir = $PSScriptRoot
$LogsDir = Resolve-Path "$ScriptDir\..\log"

if (-not (Test-Path $LogsDir)) {
    Write-Error "Logs directory not found at $LogsDir"
    exit 1
}

Set-Location $LogsDir

# --- UI Input ---
$InputReason = [Microsoft.VisualBasic.Interaction]::InputBox("Can you give a description of the error symptoms?", "Crash Report", "")

if ([string]::IsNullOrWhiteSpace($InputReason)) {
    exit
}

$InputReason | Out-File "reason.txt" -Encoding UTF8

# --- Prepare Filenames ---
$DateStr = Get-Date -Format "yyyyMMdd_HHmmss_zzz" # zzz for timezone offset
# Sanitize timezone string for filename (replace : with nothing)
$DateStr = $DateStr -replace ":",""
$BaseName = "CrashReport-$DateStr"
$DesktopPath = [Environment]::GetFolderPath("Desktop")
$OutputTgz = Join-Path $DesktopPath "$BaseName.tgz"
$OutputTar = Join-Path $DesktopPath "$BaseName.tar"

# --- Identify Files to Archive ---

# 1. Latest output folder
$LatestOutput = Get-ChildItem -Path "output" -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1

# 2. Latest 4 images
$ImagesToArchive = @()
if (Test-Path "images") {
    $ImagesToArchive = Get-ChildItem -Path "images" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 4
}

# 3. Logs
$DebugLog = if (Test-Path "debug.log") { "debug.log" } else { $null }
$Debug1Log = if (Test-Path "debug1.log") { "debug1.log" } else { $null }

# --- Create Archive ---
# We use a temporary directory to stage files to replicate 'tar uf' behavior cleanly
$TempDir = Join-Path $env:TEMP "NeptusCrashReport_$((Get-Date).Ticks)"
New-Item -Path $TempDir -ItemType Directory | Out-Null

try {
    # Copy Reason
    Copy-Item "reason.txt" $TempDir

    # Copy Output
    if ($LatestOutput) {
        $DestOut = Join-Path $TempDir "output\$($LatestOutput.Name)"
        New-Item -ItemType Directory -Path (Split-Path $DestOut) -Force | Out-Null
        Copy-Item -Path $LatestOutput.FullName -Destination $DestOut -Recurse
    }

    # Copy Logs
    if ($DebugLog) { Copy-Item $DebugLog $TempDir }
    if ($Debug1Log) { Copy-Item $Debug1Log $TempDir }

    # Copy Images
    if ($ImagesToArchive) {
        $ImgDest = Join-Path $TempDir "images"
        New-Item -ItemType Directory -Path $ImgDest -Force | Out-Null
        foreach ($img in $ImagesToArchive) {
            Copy-Item $img.FullName $ImgDest
        }
    }

    # Compress
    # Check for native tar (Win10+) or 7z
    if (Get-Command "tar" -ErrorAction SilentlyContinue) {
        # Use native tar to create tgz directly
        # -C changes dir to temp, -c create, -z gzip, -f file
        tar -czf "$OutputTgz" -C "$TempDir" .
    }
    elseif (Get-Command "7z" -ErrorAction SilentlyContinue) {
        # Use 7zip
        & 7z a -ttar "$OutputTar" "$TempDir\*" | Out-Null
        & 7z a -tgzip "$OutputTgz" "$OutputTar" | Out-Null
        Remove-Item "$OutputTar" -Force
    }
    else {
        # Fallback to standard Zip if no tar/7z (changes format extension!)
        $OutputZip = Join-Path $DesktopPath "$BaseName.zip"
        Compress-Archive -Path "$TempDir\*" -DestinationPath $OutputZip
        $OutputTgz = $OutputZip # For the notification message
    }

} finally {
    # Cleanup
    Remove-Item "reason.txt" -Force
    Remove-Item $TempDir -Recurse -Force
}

# --- Notification ---
[System.Windows.Forms.MessageBox]::Show("Generated $OutputTgz. Don't forget to send this archive to Neptus development team.", "Crash Report Generated", [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information)
