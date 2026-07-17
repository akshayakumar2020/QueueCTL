$ErrorActionPreference = "Stop"

$baseDir = Split-Path $MyInvocation.MyCommand.Path
Set-Location $baseDir

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "QueueCTL Demo" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Project initialized successfully." -ForegroundColor Green
Write-Host ""
Write-Host "Now use QueueCTL manually." -ForegroundColor Yellow
Write-Host ""
Write-Host "Example commands:" -ForegroundColor White
Write-Host ""
Write-Host "  queuectl enqueue '`{`"id`":`"job1`",`"command`":`"echo Hello`"`}'"
Write-Host "  queuectl list"
Write-Host "  queuectl worker start --count 2"
Write-Host "  queuectl status"
Write-Host "  queuectl dlq list"
Write-Host "  queuectl worker stop"
Write-Host ""
