$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$pidFile = Join-Path $projectRoot ".run\pids.json"

if (Test-Path -LiteralPath $pidFile) {
    $entries = Get-Content -LiteralPath $pidFile -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $entries) {
        $processId = [int]$entry.pid
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            try {
                & taskkill.exe /PID "$processId" /T /F 2>$null | Out-Null
            } catch {
                Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            }
            Wait-Process -Id $processId -Timeout 10 -ErrorAction SilentlyContinue
            Write-Host "Stopped launcher for $($entry.name) (PID $processId)."
        }
    }

    Remove-Item -LiteralPath $pidFile
} else {
    Write-Host "No process list found; checking MindCare service ports..."
}

$managedPorts = @(
    [PSCustomObject]@{
        Name = "api-gateway"
        Port = 8079
        CommandPattern = "com.mindcare.api_gateway.ApiGatewayApplication"
    },
    [PSCustomObject]@{
        Name = "auth-service"
        Port = 8081
        CommandPattern = "com.mindcare.auth_service.AuthServiceApplication"
    },
    [PSCustomObject]@{
        Name = "booking-service"
        Port = 8082
        CommandPattern = "com.mindcare.bookingservice.BookingServiceApplication"
    },
    [PSCustomObject]@{
        Name = "emotion-service"
        Port = 8083
        CommandPattern = "com.mindcare.emotionservice.EmotionServiceApplication"
    },
    [PSCustomObject]@{
        Name = "ai-service"
        Port = 8084
        CommandPattern = "com.mindcare.ai_service.AiServiceApplication"
    },
    [PSCustomObject]@{
        Name = "mindcare-web-app"
        Port = 5173
        CommandPattern = "mindcare-frontend\web-app"
    }
)

foreach ($managedPort in $managedPorts) {
    $listeners = @(
        Get-NetTCPConnection `
            -State Listen `
            -LocalPort $managedPort.Port `
            -ErrorAction SilentlyContinue
    )

    foreach ($listener in $listeners) {
        $processId = [int]$listener.OwningProcess
        $processDetails = Get-CimInstance `
            -ClassName Win32_Process `
            -Filter "ProcessId = $processId" `
            -ErrorAction SilentlyContinue
        $commandLine = [string]$processDetails.CommandLine

        if ($commandLine -notlike "*$($managedPort.CommandPattern)*") {
            Write-Warning (
                "Port $($managedPort.Port) is used by PID $processId, " +
                "but it is not the expected $($managedPort.Name) process. It was not stopped."
            )
            continue
        }

        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $processId -Timeout 10 -ErrorAction SilentlyContinue
        Write-Host "Stopped $($managedPort.Name) on port $($managedPort.Port) (PID $processId)."
    }
}

Write-Host "Application processes, including mindcare-frontend/web-app, have been stopped."
Write-Host "PostgreSQL and pgAdmin are still running to preserve data."
Write-Host "Mailpit is also still running. Use 'docker compose -f infrastructure\docker-compose.yml down' to stop infrastructure."
