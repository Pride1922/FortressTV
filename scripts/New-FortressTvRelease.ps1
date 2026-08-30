[CmdletBinding()]
param(
    [string]$ReleaseNotes = "FortressTV update",
    [string]$ReleaseDirectory = "C:\Users\fabio\Nextcloud\FortressTV",
    [string]$DownloadUrl = "https://cloud.ameyerocha.com/s/2TYXawD6kCQ62iN/download"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Net.Http

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$releaseRoot = [System.IO.Path]::GetFullPath($ReleaseDirectory)
$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"
$keystoreProperties = Join-Path $projectRoot "keystore.properties"
$ffmpegSourceRoot = Join-Path $projectRoot "third_party\ffmpeg"

if (-not (Test-Path -LiteralPath $releaseRoot -PathType Container)) {
    throw "Release directory does not exist: $releaseRoot"
}
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
$publishedSourceRoot = Join-Path $releaseRoot "source"

# Overwrite the existing paths so Nextcloud keeps the established share links.
Copy-Item -LiteralPath $builtApk.FullName -Destination $publishedApk -Force
$apkHash = (Get-FileHash -LiteralPath $publishedApk -Algorithm SHA256).Hash

# Nextcloud may publish the small JSON before the larger APK finishes syncing.
# Wait until the public APK link serves the exact file before announcing the release.
$remoteHash = $null
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.DefaultRequestHeaders.CacheControl = [System.Net.Http.Headers.CacheControlHeaderValue]::new()
$httpClient.DefaultRequestHeaders.CacheControl.NoCache = $true
try {
    for ($attempt = 1; $attempt -le 12; $attempt++) {
        try {
            $separator = if ($DownloadUrl.Contains("?")) { "&" } else { "?" }
            $cacheBustedUrl = $DownloadUrl + $separator + "releaseCheck=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
            $stream = $httpClient.GetStreamAsync($cacheBustedUrl).GetAwaiter().GetResult()
            try {
                $digest = [System.Security.Cryptography.SHA256]::Create()
                try {
                    $remoteHash = ([System.BitConverter]::ToString($digest.ComputeHash($stream))).Replace("-", "")
                }
                finally {
                    $digest.Dispose()
                }
            }
            finally {
                $stream.Dispose()
            }
        }
        catch {
            $remoteHash = $null
        }

        if ($remoteHash -eq $apkHash) {
            break
        }
        if ($attempt -lt 12) {
            Write-Host "Waiting for Nextcloud to publish the new APK ($attempt/12)..."
            Start-Sleep -Seconds 5
        }
    }
}
finally {
    $httpClient.Dispose()
}

if ($remoteHash -ne $apkHash) {
    throw "The public APK link did not update to the new checksum. version.json was left unchanged."
}

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

# Keep the exact LGPL corresponding source and build instructions beside the APK.
New-Item -ItemType Directory -Path $publishedSourceRoot -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $ffmpegSourceRoot "ffmpeg-6.0-source.tar.gz") -Destination $publishedSourceRoot -Force
Copy-Item -LiteralPath (Join-Path $ffmpegSourceRoot "COPYING.LGPLv2.1") -Destination $publishedSourceRoot -Force
Copy-Item -LiteralPath (Join-Path $ffmpegSourceRoot "build-config.txt") -Destination $publishedSourceRoot -Force
Copy-Item -LiteralPath (Join-Path $ffmpegSourceRoot "README.md") -Destination $publishedSourceRoot -Force
Copy-Item -LiteralPath (Join-Path $projectRoot "THIRD_PARTY_NOTICES.md") -Destination $publishedSourceRoot -Force

$publishedFile = Get-Item -LiteralPath $publishedApk
Write-Host ""
Write-Host "FortressTV $versionName (versionCode $versionCode) published successfully."
Write-Host "APK: $($publishedFile.FullName)"
Write-Host "Size: $([math]::Round($publishedFile.Length / 1MB, 2)) MB"
Write-Host "SHA-256: $apkHash"
Write-Host "Metadata: $publishedJson"
Write-Host "Corresponding source: $publishedSourceRoot"
