$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$pidFile = Join-Path $projectRoot ".run\pids.json"

if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host "No process list found at .run\pids.json."
    exit 0
}

$entries = @(Get-Content -LiteralPath $pidFile -Raw -Encoding UTF8 | ConvertFrom-Json)
foreach ($entry in $entries) {
    $process = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $entry.pid
        Write-Host "Stopped $($entry.name) (PID $($entry.pid))."
    }
}

Remove-Item -LiteralPath $pidFile
Write-Host "PostgreSQL and pgAdmin are still running to preserve data."
Write-Host "Mailpit is also still running. Use 'docker compose -f infrastructure\docker-compose.yml down' to stop infrastructure."
