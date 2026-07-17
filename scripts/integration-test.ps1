$ErrorActionPreference = "Stop"

Set-Location (Resolve-Path "$PSScriptRoot\..")

mvn -q clean package
Remove-Item -Recurse -Force .\data -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force .\data | Out-Null

@'
{"id":"e2e-ok","command":"echo e2e-ok"}
'@ | Set-Content -Path .\data\e2e-ok.json

@'
{"id":"e2e-dead","command":"cmd /c exit 1","max_retries":2}
'@ | Set-Content -Path .\data\e2e-dead.json

.\bin\queuectl.ps1 config set backoff-base 1
.\bin\queuectl.ps1 enqueue '@data/e2e-ok.json'
.\bin\queuectl.ps1 enqueue '@data/e2e-dead.json'
.\bin\queuectl.ps1 worker start --count 2
Start-Sleep -Seconds 5
.\bin\queuectl.ps1 worker stop

$listOutput = .\bin\queuectl.ps1 list
$dlqOutput = .\bin\queuectl.ps1 dlq list
$listText = $listOutput -join "`n"
$dlqText = $dlqOutput -join "`n"

$listOutput
$dlqOutput

if ($listText -notmatch "e2e-ok\s+completed") {
    throw "completed job was not found"
}
if ($listText -notmatch "e2e-dead\s+dead") {
    throw "dead job was not found"
}
if ($dlqText -notmatch "e2e-dead") {
    throw "DLQ job was not listed"
}

Write-Output "integration test passed"
