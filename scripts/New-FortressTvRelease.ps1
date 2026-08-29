[CmdletBinding()]
param(
    [string]$ReleaseNotes = "FortressTV update",
    [string]$ReleaseDirectory = "C:\Users\fabio\Nextcloud\FortressTV",
    [string]$DownloadUrl = "https://cloud.ameyerocha.com/s/2TYXawD6kCQ62iN/download"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$releaseRoot = [System.IO.Path]::GetFullPath($ReleaseDirectory)
$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"
$keystoreProperties = Join-Path $projectRoot "keystore.properties"

if (-not (Test-Path -LiteralPath $releaseRoot -PathType Container)) {
    throw "Release directory does not exist: $releaseRoot"
}
if (-not (Test-Path -LiteralPath $keystoreProperties -PathType Leaf)) {
    throw "keystore.properties is missing. A signed release cannot be created."
}
if (-not $DownloadUrl.StartsWith("https://", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "The APK download URL must use HTTPS."
}

$gradleText = Get-Content -LiteralPath $gradleFile -Raw
$versionNameMatch = [regex]::Match($gradleText, 'versionName\s*=\s*"([^"]+)"')
$versionCodeMatch = [regex]::Match($gradleText, 'versionCode\s*=\s*(\d+)')
if (-not $versionNameMatch.Success -or -not $versionCodeMatch.Success) {
    throw "Could not read versionName and versionCode from app/build.gradle.kts."
}
$versionName = $versionNameMatch.Groups[1].Value
$versionCode = [int]$versionCodeMatch.Groups[1].Value

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

$publishedApk = Join-Path $releaseRoot "fortresstv.apk"
$publishedJson = Join-Path $releaseRoot "version.json"

# Overwrite the existing paths so Nextcloud keeps the established share links.
Copy-Item -LiteralPath $builtApk.FullName -Destination $publishedApk -Force
$apkHash = (Get-FileHash -LiteralPath $publishedApk -Algorithm SHA256).Hash

$metadata = [ordered]@{
    version = $versionName
    versionCode = $versionCode
    notes = $ReleaseNotes
    downloadUrl = $DownloadUrl
    sha256 = $apkHash
}
$json = $metadata | ConvertTo-Json -Depth 3

# Validate the generated JSON before publishing it.
$null = $json | ConvertFrom-Json
[System.IO.File]::WriteAllText($publishedJson, $json + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
$null = (Get-Content -LiteralPath $publishedJson -Raw) | ConvertFrom-Json

$publishedFile = Get-Item -LiteralPath $publishedApk
Write-Host ""
Write-Host "FortressTV $versionName (versionCode $versionCode) published successfully."
Write-Host "APK: $($publishedFile.FullName)"
Write-Host "Size: $([math]::Round($publishedFile.Length / 1MB, 2)) MB"
Write-Host "SHA-256: $apkHash"
Write-Host "Metadata: $publishedJson"
