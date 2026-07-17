$ErrorActionPreference = "Stop"

$rootDir = Split-Path $PSScriptRoot
$jarPath = Join-Path $rootDir "target\queuectl.jar"

if (-Not (Test-Path $jarPath)) {
    Write-Host "QueueCTL installation not found." -ForegroundColor Red
    Write-Host ""
    Write-Host "Expected:"
    Write-Host "bin\queuectl.ps1"
    Write-Host "target\queuectl.jar"
    Write-Host ""
    Write-Host "Please run:"
    Write-Host "mvn clean package" -ForegroundColor Yellow
    exit 1
}

[array]$escapedArgs = $args | ForEach-Object { $_ -replace '"', '\"' }
& java -jar $jarPath @escapedArgs
exit $LASTEXITCODE
