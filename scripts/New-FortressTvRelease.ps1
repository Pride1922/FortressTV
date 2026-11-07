[CmdletBinding()]
param(
    [string]$ReleaseNotes = "FortressTV update",
    [string]$ReleaseDirectory = "",
    [string]$DownloadUrl = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"
$keystoreProperties = Join-Path $projectRoot "keystore.properties"
$ffmpegSourceRoot = Join-Path $projectRoot "third_party\ffmpeg"

if (-not (Test-Path -LiteralPath $keystoreProperties -PathType Leaf)) {
    throw "keystore.properties is missing. A signed release cannot be created."
}
$requiredSourceFiles = @(
    "ffmpeg-6.0-source.tar.gz",
    "COPYING.LGPLv2.1",
    "build-config.txt",
    "README.md"
)
foreach ($sourceFile in $requiredSourceFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $ffmpegSourceRoot $sourceFile) -PathType Leaf)) {
        throw "Required FFmpeg corresponding-source file is missing: $sourceFile"
    }
}

$gradleText = Get-Content -LiteralPath $gradleFile -Raw
$versionNameMatch = [regex]::Match($gradleText, 'versionName\s*=\s*"([^"]+)"')
$versionCodeMatch = [regex]::Match($gradleText, 'versionCode\s*=\s*(\d+)')
if (-not $versionNameMatch.Success -or -not $versionCodeMatch.Success) {
    throw "Could not read versionName and versionCode from app/build.gradle.kts."
}
$versionName = $versionNameMatch.Groups[1].Value
$versionCode = [int]$versionCodeMatch.Groups[1].Value

if ([string]::IsNullOrWhiteSpace($DownloadUrl)) {
    $DownloadUrl = "https://github.com/Pride1922/FortressTV/releases/download/v$versionName/fortresstv.apk"
}
if (-not $DownloadUrl.StartsWith("https://", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "The APK download URL must use HTTPS."
}

Push-Location $projectRoot
try {
    & ".\gradlew.bat" clean testDebugUnitTest assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "The release build failed."
    }
}
finally {
    Pop-Location
}

$builtApk = Get-ChildItem -LiteralPath (Join-Path $projectRoot "app\build\outputs\apk\release") -File -Filter "*.apk" |
    Where-Object { $_.Name -notlike "*unsigned*" } |
    Select-Object -First 1
if ($null -eq $builtApk) {
    throw "Gradle did not produce a signed release APK."
}

$buildToolsRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"
$buildTools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
$apkSigner = Join-Path $buildTools.FullName "apksigner.bat"
& $apkSigner verify --verbose $builtApk.FullName
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed. Nothing was published."
}

$apkHash = (Get-FileHash -LiteralPath $builtApk.FullName -Algorithm SHA256).Hash

$metadata = [ordered]@{
    version = $versionName
    versionCode = $versionCode
    notes = $ReleaseNotes
    downloadUrl = $DownloadUrl
    sha256 = $apkHash
}
$json = $metadata | ConvertTo-Json -Depth 3

# Write version.json into repository root for raw.githubusercontent.com hosting
$rootJson = Join-Path $projectRoot "version.json"
[System.IO.File]::WriteAllText($rootJson, $json + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

# Create a staging folder with standard fortresstv.apk naming
$stagingDir = Join-Path $projectRoot "work\release-staging"
if (Test-Path -LiteralPath $stagingDir) {
    Remove-Item -LiteralPath $stagingDir -Recurse -Force
}
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
$stagedApk = Join-Path $stagingDir "fortresstv.apk"
Copy-Item -LiteralPath $builtApk.FullName -Destination $stagedApk -Force

# Publish directly to GitHub Releases via gh CLI
Write-Host "Publishing release v$versionName to GitHub..."
$sourceAssets = @(
    $stagedApk,
    $rootJson,
    (Join-Path $ffmpegSourceRoot "ffmpeg-6.0-source.tar.gz"),
    (Join-Path $ffmpegSourceRoot "COPYING.LGPLv2.1"),
    (Join-Path $ffmpegSourceRoot "build-config.txt"),
    (Join-Path $ffmpegSourceRoot "README.md"),
    (Join-Path $projectRoot "THIRD_PARTY_NOTICES.md")
)

& gh release create "v$versionName" @sourceAssets `
    --title "FortressTV $versionName" `
    --notes "$ReleaseNotes" `
    --latest

if ($LASTEXITCODE -ne 0) {
    Write-Warning "gh release create returned non-zero code. If the release already exists, consider running 'gh release upload'."
}

# Optional local backup copy if directory was specified
if (-not [string]::IsNullOrWhiteSpace($ReleaseDirectory) -and (Test-Path -LiteralPath $ReleaseDirectory -PathType Container)) {
    Copy-Item -LiteralPath $stagedApk -Destination (Join-Path $ReleaseDirectory "fortresstv.apk") -Force
    Copy-Item -LiteralPath $rootJson -Destination (Join-Path $ReleaseDirectory "version.json") -Force
}

Write-Host ""
Write-Host "FortressTV $versionName (versionCode $versionCode) published successfully to GitHub Releases."
Write-Host "Tag: v$versionName"
Write-Host "Download URL: $DownloadUrl"
Write-Host "SHA-256: $apkHash"
Write-Host "Metadata: $rootJson"
