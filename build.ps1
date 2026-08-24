[CmdletBinding()]
param(
    [string]$MavenCommand = "mvn.cmd",
    [switch]$Clean,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if (Test-Path -LiteralPath $MavenCommand -PathType Leaf) {
    $mavenExecutable = (Resolve-Path -LiteralPath $MavenCommand).Path
} else {
    $command = Get-Command $MavenCommand -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Maven executable '$MavenCommand' was not found. Add mvn.cmd to PATH or pass -MavenCommand."
    }
    $mavenExecutable = $command.Source
}

$mavenArguments = @("clean")
if (-not $Clean) {
    $mavenArguments += "verify"
}
if (-not $Clean -and $SkipTests) {
    $mavenArguments += "-DskipTests"
}

Push-Location $projectRoot
try {
    if ($Clean) {
        Write-Host "Cleaning Maven build artifacts in $projectRoot"
    } else {
        Write-Host "Running Maven build in $projectRoot"
    }
    & $mavenExecutable @mavenArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    if ($Clean) {
        Write-Host "Maven build artifacts cleaned." -ForegroundColor Green
        return
    }

    $jar = Get-ChildItem -LiteralPath (Join-Path $projectRoot "target") -Filter "*.jar" -File |
        Where-Object { $_.Name -notmatch "-(sources|javadoc)\.jar$" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $jar) {
        throw "Maven completed, but no application JAR was found in target."
    }

    Write-Host "JAR built: $($jar.FullName)" -ForegroundColor Green
} finally {
    Pop-Location
}
