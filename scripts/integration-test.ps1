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

java -jar target/queuectl.jar config set backoff-base 1
java -jar target/queuectl.jar enqueue '@data/e2e-ok.json'
java -jar target/queuectl.jar enqueue '@data/e2e-dead.json'
java -jar target/queuectl.jar worker start --count 2
Start-Sleep -Seconds 5
java -jar target/queuectl.jar worker stop

$listOutput = java -jar target/queuectl.jar list
$dlqOutput = java -jar target/queuectl.jar dlq list
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
