[CmdletBinding()]
param(
    [string]$Distro = "Ubuntu-24.04"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$linuxProjectRoot = (& wsl -d $Distro -- wslpath -a $projectRoot).Trim()
if (-not $linuxProjectRoot) {
    throw "Could not resolve the FortressTV project path inside WSL."
}

& wsl -d $Distro -- bash "$linuxProjectRoot/scripts/build-media3-ffmpeg.sh"
if ($LASTEXITCODE -ne 0) {
    throw "The Media3 FFmpeg decoder build failed."
}
