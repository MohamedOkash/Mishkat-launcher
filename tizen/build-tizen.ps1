# Package Mishkat Launcher for Samsung Smart TV (Tizen OS)
param(
    [string]$OutputDir = "$PSScriptRoot/../dist"
)

$ErrorActionPreference = "Stop"
Write-Host "=== Packaging Mishkat for Samsung Smart TV (Tizen) ===" -ForegroundColor Cyan

# 1. Ensure output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$stageDir = "$PSScriptRoot/stage"
if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

# 2. Copy Tizen manifest and branding
Copy-Item "$PSScriptRoot/config.xml" "$stageDir/"
Copy-Item "$PSScriptRoot/icon.png" "$stageDir/"

# 3. Copy Web App Core
Copy-Item "$PSScriptRoot/../tv-web/index.html" "$stageDir/"
Copy-Item -Recurse "$PSScriptRoot/../tv-web/css" "$stageDir/"
Copy-Item -Recurse "$PSScriptRoot/../tv-web/js" "$stageDir/"

# 4. Copy Shared Resources
$sharedStage = "$stageDir/shared"
New-Item -ItemType Directory -Path $sharedStage -Force | Out-Null
Copy-Item "$PSScriptRoot/../shared/*.json" "$sharedStage/"

# 5. Build .wgt (Tizen Widget Container)
$wgtPath = "$OutputDir/MishkatLauncher-Samsung-Tizen.wgt"
if (Test-Path $wgtPath) {
    Remove-Item -Force $wgtPath
}

# Check if Tizen CLI exists
$tizenCli = Get-Command "tizen" -ErrorAction SilentlyContinue
if ($tizenCli) {
    Write-Host "Using Samsung Tizen CLI..." -ForegroundColor Green
    & tizen package -t wgt -s -- "$stageDir" -o "$OutputDir"
} else {
    Write-Host "Tizen CLI not in PATH - Assembling standard .wgt archive..." -ForegroundColor Yellow
    # A .wgt is a standard ZIP archive containing config.xml at the root
    $tempZip = "$OutputDir/mishkat_tizen_temp.zip"
    if (Test-Path $tempZip) { Remove-Item -Force $tempZip }
    Compress-Archive -Path "$stageDir/*" -DestinationPath "$tempZip" -Force
    Move-Item -Force "$tempZip" "$wgtPath"
}

# Cleanup staging
Remove-Item -Recurse -Force $stageDir

if (Test-Path $wgtPath) {
    $size = (Get-Item $wgtPath).Length / 1KB
    Write-Host "Successfully generated Samsung Tizen package: $wgtPath ($([math]::Round($size, 2)) KB)" -ForegroundColor Green
} else {
    Write-Error "Failed to generate $wgtPath"
}
