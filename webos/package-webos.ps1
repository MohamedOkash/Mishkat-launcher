# Package Mishkat Launcher for LG Smart TV (webOS)
param(
    [string]$OutputDir = "$PSScriptRoot/../dist"
)

$ErrorActionPreference = "Stop"
Write-Host "=== Packaging Mishkat for LG Smart TV (webOS) ===" -ForegroundColor Cyan

# 1. Ensure output directory
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$stageDir = "$PSScriptRoot/stage"
if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

# 2. Copy webOS manifest and assets
Copy-Item "$PSScriptRoot/appinfo.json" "$stageDir/"
Copy-Item "$PSScriptRoot/icon.png" "$stageDir/"
Copy-Item "$PSScriptRoot/largeIcon.png" "$stageDir/"
Copy-Item "$PSScriptRoot/splash.png" "$stageDir/"

# 3. Copy Web App Core
Copy-Item "$PSScriptRoot/../tv-web/index.html" "$stageDir/"
Copy-Item -Recurse "$PSScriptRoot/../tv-web/css" "$stageDir/"
Copy-Item -Recurse "$PSScriptRoot/../tv-web/js" "$stageDir/"

# 4. Copy Shared Resources
$sharedStage = "$stageDir/shared"
New-Item -ItemType Directory -Path $sharedStage -Force | Out-Null
Copy-Item "$PSScriptRoot/../shared/*.json" "$sharedStage/"

# 5. Build .ipk or deployable package
$ipkPath = "$OutputDir/com.mishkat.launcher_1.6.0_all.ipk"
$zipPath = "$OutputDir/MishkatLauncher-LG-webOS.zip"

$aresPackage = Get-Command "ares-package" -ErrorAction SilentlyContinue
if ($aresPackage) {
    Write-Host "Using LG webOS ares-package CLI..." -ForegroundColor Green
    & ares-package "$stageDir" -o "$OutputDir"
} else {
    Write-Host "Assembling deployable webOS .ipk and archive..." -ForegroundColor Yellow
    python "$PSScriptRoot/build-ipk.py" "$stageDir" "$ipkPath"
    Compress-Archive -Path "$stageDir/*" -DestinationPath "$zipPath" -Force
}

# Cleanup staging
Remove-Item -Recurse -Force $stageDir

Write-Host "LG Smart TV webOS package prepared in $OutputDir" -ForegroundColor Green
