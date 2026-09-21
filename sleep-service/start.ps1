$ErrorActionPreference = 'Stop'
$sleepPython = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $sleepPython)) {
    throw 'Missing Python environment. Follow sleep-service/README.md to install dependencies.'
}
Push-Location $PSScriptRoot
try {
    & $sleepPython -m uvicorn app:app --host 127.0.0.1 --port 8085
    if ($LASTEXITCODE -ne 0) { throw 'Sleep inference service failed to start.' }
} finally {
    Pop-Location
}
