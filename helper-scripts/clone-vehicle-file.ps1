<#
.SYNOPSIS
    Clones a Neptus vehicle definition file (.nvcl) and updates specific parameters.

.DESCRIPTION
    This script reads an existing XML vehicle configuration file, updates the ID, Name, 
    IMEI, IP Address, IMC-ID, and Icon Color, and saves it to a new file. 

.EXAMPLE
    .\Clone-Vehicle.ps1 -SourceFile "vehicles-defs\09-autonaut-01.nvcl" `
                        -Id "autonaut-02" `
                        -Name "Autonaut 02" `
                        -Imei "300534068640999" `
                        -Imei1 "300534068644999" `
                        -Ip "10.1.0.2" `
                        -ImcId "08:06" `
                        -IconColor "255,0,0"
#>

[CmdletBinding()]
param (
    [Parameter(Mandatory=$false)]
    [string]$SourceFile = "vehicles-defs\09-autonaut-01.nvcl",

    [Parameter(Mandatory=$true, HelpMessage="All lowercase vehicle name, no spaces (e.g., autonaut-02)")]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Id,

    [Parameter(Mandatory=$true, HelpMessage="Full name, mix case and spaces allowed (e.g., Autonaut 02)")]
    [string]$Name,

    [Parameter(Mandatory=$true, HelpMessage="Primary IMEI number")]
    [string]$Imei,

    [Parameter(Mandatory=$false, HelpMessage="Secondary IMEI number (Optional)")]
    [string]$Imei1,

    [Parameter(Mandatory=$true, HelpMessage="IP address of the vehicle")]
    [string]$Ip,

    [Parameter(Mandatory=$true, HelpMessage="IMC id in hex format (e.g., 08:06)")]
    [ValidatePattern('^[0-9a-fA-F]{2}:[0-9a-fA-F]{2}$')]
    [string]$ImcId,

    [Parameter(Mandatory=$false, HelpMessage="Optional RGB icon color (e.g., '255,51,153')")]
    [ValidatePattern('^\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*$')]
    [string]$IconColor,

    [Parameter(Mandatory=$false, HelpMessage="Path for the output file. Defaults to creating a new file alongside the source.")]
    [string]$DestinationFile
)

# 1. Check if source file exists and resolve its path securely
if (-not (Test-Path $SourceFile)) {
    throw "Source file not found at: '$SourceFile'. Please verify the path and file exist."
}
$SourcePath = Convert-Path $SourceFile

# 2. Determine Destination File if not provided
if ([string]::IsNullOrWhiteSpace($DestinationFile)) {
    $parentDir = Split-Path $SourcePath -Parent
    $extension = [System.IO.Path]::GetExtension($SourcePath)
    $DestinationFile = Join-Path $parentDir "$Id$extension"
}

Write-Host "Reading source file: $SourcePath" -ForegroundColor Cyan

# 3. Load the XML file
[xml]$xml = Get-Content $SourcePath

# 4. Modify the required fields using XPath

# -> properties > id
$nodeId = $xml.SelectSingleNode("/system/properties/id")
if ($nodeId) { $nodeId.InnerText = $Id }

# -> properties > name
$nodeName = $xml.SelectSingleNode("/system/properties/name")
if ($nodeName) { $nodeName.InnerText = $Name }

# -> properties > appearance > icon-color (Optional)
if ($IconColor) {
    $colors = $IconColor -split ','
    $nodeR = $xml.SelectSingleNode("/system/properties/appearance/icon-color/r")
    $nodeG = $xml.SelectSingleNode("/system/properties/appearance/icon-color/g")
    $nodeB = $xml.SelectSingleNode("/system/properties/appearance/icon-color/b")
    
    if ($nodeR -and $nodeG -and $nodeB) {
        $nodeR.InnerText = $colors[0].Trim()
        $nodeG.InnerText = $colors[1].Trim()
        $nodeB.InnerText = $colors[2].Trim()
    } else {
        Write-Warning "Could not find <icon-color> nodes in the source XML to update."
    }
}

# -> protocols-supported > protocols-args > iridium > imei / imei1
$nodeImei = $xml.SelectSingleNode("/system/protocols-supported/protocols-args/iridium/imei")
if ($nodeImei) { $nodeImei.InnerText = $Imei }

if ($Imei1) {
    $nodeImei1 = $xml.SelectSingleNode("/system/protocols-supported/protocols-args/iridium/imei1")
    if ($nodeImei1) { 
        $nodeImei1.InnerText = $Imei1 
    } else {
        # If imei1 doesn't exist in the template, we create it
        $iridiumNode = $xml.SelectSingleNode("/system/protocols-supported/protocols-args/iridium")
        if ($iridiumNode) {
            $newImei1Node = $xml.CreateElement("imei1")
            $newImei1Node.InnerText = $Imei1
            $iridiumNode.AppendChild($newImei1Node) | Out-Null
        }
    }
} else {
    # If Imei1 is not provided, remove the node from the XML if it exists
    $nodeImei1 = $xml.SelectSingleNode("/system/protocols-supported/protocols-args/iridium/imei1")
    if ($nodeImei1) {
        $nodeImei1.ParentNode.RemoveChild($nodeImei1) | Out-Null
    }
}

# -> communication-means > comm-mean > host-address
# We grab the first one assuming standard structure
$nodeIp = $xml.SelectSingleNode("/system/communication-means/comm-mean/host-address")
if ($nodeIp) { $nodeIp.InnerText = $Ip }

# -> communication-means > comm-mean > protocols-args > imc > imc-id
$nodeImcId = $xml.SelectSingleNode("/system/communication-means/comm-mean/protocols-args/imc/imc-id")
if ($nodeImcId) { $nodeImcId.InnerText = $ImcId }

# 5. Save the modified XML with clean UTF-8 Encoding (No Byte-Order-Mark)
if ([System.IO.Path]::IsPathRooted($DestinationFile)) {
    $DestPath = $DestinationFile
} else {
    $DestPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $DestinationFile))
}

$XmlWriterSettings = New-Object System.Xml.XmlWriterSettings
$XmlWriterSettings.Indent = $true
$XmlWriterSettings.Encoding = New-Object System.Text.UTF8Encoding($false) # $false omits the BOM

$XmlWriter = [System.Xml.XmlWriter]::Create($DestPath, $XmlWriterSettings)
$xml.Save($XmlWriter)
$XmlWriter.Close()

Write-Host "Successfully cloned vehicle definition to: $DestPath" -ForegroundColor Green
