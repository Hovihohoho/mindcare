param(
    [switch]$SkipAi,
    [switch]$RestartExisting,
    [switch]$SeedDemo,
    [switch]$UseRealEmail,
    [switch]$UseMailpit
)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"
$runDirectory = Join-Path $projectRoot ".run"
$frontendDirectory = Join-Path $projectRoot "mindcare-frontend\web-app"

if ($SeedDemo) {
    $env:SEED_ASSESSMENTS = "true"
}

if (-not (Test-Path -LiteralPath (Join-Path $frontendDirectory "package.json"))) {
    throw "Missing frontend project at $frontendDirectory."
}

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing $Path. Copy .env.example to .env and fill in the configuration."
    }

    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            throw "Invalid line in .env: $line"
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Require-EnvironmentValue {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value) -or $value.StartsWith("your-")) {
        throw "Missing $Name in .env."
    }
}

function Get-MavenExecutable {
    $installed = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($installed) {
        return $installed.Source
    }

    $wrapperDirectory = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
    if (Test-Path -LiteralPath $wrapperDirectory) {
        $cached = Get-ChildItem -LiteralPath $wrapperDirectory -Recurse -Filter "mvn.cmd" |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($cached) {
            return $cached.FullName
        }
    }

    throw "Maven was not found. Run a project mvnw.cmd once to download Maven."
}

function Test-PortInUse {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Stop-PortListener {
    param([int]$Port)

    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping PID $($process.Id) ($($process.ProcessName)) on port $Port..."
            Stop-Process -Id $process.Id
            Wait-Process -Id $process.Id -Timeout 10 -ErrorAction SilentlyContinue
        }
    }
}

function Start-LoggedProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory
    )

    $stdout = Join-Path $runDirectory "$Name.log"
    $stderr = Join-Path $runDirectory "$Name.error.log"
    $process = Start-Process -FilePath $FilePath `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    Write-Host "Started $Name (PID $($process.Id)). Log: $stdout"
    return [PSCustomObject]@{ name = $Name; pid = $process.Id }
}

Import-DotEnv -Path $envFile

# Spring Boot interprets a machine-level DEBUG variable as its global debug
# switch. Override it for local runs to keep demo logs concise and avoid
# logging sensitive WebSocket query parameters.
$env:DEBUG = "false"

if ($UseRealEmail -and $UseMailpit) {
    throw "Use only one email mode: -UseRealEmail or -UseMailpit."
}
$realEmailEnabled = $UseRealEmail -or
    (-not $UseMailpit -and $env:SPRING_PROFILES_ACTIVE -eq "real-email")

if ($realEmailEnabled) {
    $env:SPRING_PROFILES_ACTIVE = "real-email"
    Require-EnvironmentValue "SMTP_USERNAME"
    Require-EnvironmentValue "SMTP_PASSWORD"
    if ([string]::IsNullOrWhiteSpace($env:MAIL_FROM)) {
        $env:MAIL_FROM = $env:SMTP_USERNAME
    }
} else {
    $env:SPRING_PROFILES_ACTIVE = "default"
    $env:SMTP_HOST = "localhost"
    $env:SMTP_PORT = "1025"
    $env:SMTP_USERNAME = ""
    $env:SMTP_PASSWORD = ""
    $env:SMTP_AUTH = "false"
    $env:SMTP_STARTTLS = "false"
    $env:MAIL_FROM = "no-reply@mindcare.local"
}
if (-not $SkipAi) {
    Require-EnvironmentValue "GEMINI_API_KEY"
}

$databasePort = 0
$databasePortValue = [Environment]::GetEnvironmentVariable("POSTGRES_PORT", "Process")
if ([string]::IsNullOrWhiteSpace($databasePortValue)) {
    $databasePort = 55432
    [Environment]::SetEnvironmentVariable("POSTGRES_PORT", $databasePort.ToString(), "Process")
} elseif (-not [int]::TryParse($databasePortValue, [ref]$databasePort)) {
    throw "POSTGRES_PORT must be a valid TCP port."
}

if ($databasePort -lt 1 -or $databasePort -gt 65535) {
    throw "POSTGRES_PORT must be between 1 and 65535."
}

$databaseName = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) {
    "mindcare_db"
} else {
    $env:POSTGRES_DB
}
$env:DB_URL = "jdbc:postgresql://127.0.0.1:$databasePort/$databaseName"
if ([string]::IsNullOrWhiteSpace($env:BOOKING_AUTH_ADAPTER_MODE)) {
    $env:BOOKING_AUTH_ADAPTER_MODE = "auth"
}
if ([string]::IsNullOrWhiteSpace($env:AUTH_SERVICE_URL)) {
    $env:AUTH_SERVICE_URL = "http://localhost:8081"
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $env:JWT_SECRET = "TWluZENhcmUtTG9jYWwtT25seS1KV1QtU2VjcmV0LTIwMjYh"
}
if ([string]::IsNullOrWhiteSpace($env:INTERNAL_SERVICE_SECRET)) {
    $env:INTERNAL_SERVICE_SECRET = "mindcare-local-internal"
}
if ([string]::IsNullOrWhiteSpace($env:BOOKING_PAYMENT_REQUIRED)) {
    $env:BOOKING_PAYMENT_REQUIRED = "false"
}

$ports = [ordered]@{
    "API Gateway" = 8079
    "Auth Service" = 8081
    "Booking Service" = 8082
    "Emotion Service" = 8083
    "AI Service" = 8084
    "Frontend" = 5173
}
if ($SkipAi) {
    $ports.Remove("AI Service")
}

$occupied = @($ports.GetEnumerator() | Where-Object { Test-PortInUse -Port $_.Value })
if ($occupied.Count -gt 0) {
    if (-not $RestartExisting) {
        $details = ($occupied | ForEach-Object { "$($_.Key):$($_.Value)" }) -join ", "
        throw "Ports are already in use: $details. Run again with -RestartExisting."
    }
    foreach ($entry in $occupied) {
        Stop-PortListener -Port $entry.Value
    }
}

New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

Write-Host "Starting PostgreSQL on port $databasePort and pgAdmin..."
$infrastructureServices = @("postgres", "pgadmin")
if (-not $realEmailEnabled) {
    $infrastructureServices += "mailpit"
}
& docker compose --file (Join-Path $projectRoot "infrastructure\docker-compose.yml") up -d $infrastructureServices
if ($LASTEXITCODE -ne 0) {
    throw "Could not start Docker infrastructure."
}

$maven = Get-MavenExecutable
$processes = @()
$processes += Start-LoggedProcess -Name "auth-service" -FilePath $maven `
    -Arguments @("spring-boot:run") -WorkingDirectory (Join-Path $projectRoot "auth-service")

$processes += Start-LoggedProcess -Name "booking-service" -FilePath $maven `
    -Arguments @("spring-boot:run") -WorkingDirectory (Join-Path $projectRoot "booking-service")

$processes += Start-LoggedProcess -Name "emotion-service" -FilePath $maven `
    -Arguments @("spring-boot:run") -WorkingDirectory (Join-Path $projectRoot "emotion-service")

if (-not $SkipAi) {
    $processes += Start-LoggedProcess -Name "ai-service" -FilePath $maven `
        -Arguments @("spring-boot:run") -WorkingDirectory (Join-Path $projectRoot "ai-service")
}

$processes += Start-LoggedProcess -Name "api-gateway" -FilePath $maven `
    -Arguments @("spring-boot:run") -WorkingDirectory (Join-Path $projectRoot "api-gateway")

if ([string]::IsNullOrWhiteSpace($env:VITE_API_URL)) {
    $env:VITE_API_URL = "http://localhost:8079"
}

$npm = (Get-Command "npm.cmd" -ErrorAction Stop).Source
$processes += Start-LoggedProcess -Name "mindcare-web-app" -FilePath $npm `
    -Arguments @("run", "dev", "--", "--host", "127.0.0.1") `
    -WorkingDirectory $frontendDirectory

$processes | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDirectory "pids.json") -Encoding UTF8

Write-Host "Waiting for services..."
$deadline = (Get-Date).AddSeconds(90)
$exitedServices = @()
do {
    $exitedServices = @($processes | Where-Object {
        -not (Get-Process -Id $_.pid -ErrorAction SilentlyContinue)
    })
    if ($exitedServices.Count -gt 0) {
        break
    }
    $waiting = @($ports.GetEnumerator() | Where-Object { -not (Test-PortInUse -Port $_.Value) })
    if ($waiting.Count -eq 0) {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

if ($exitedServices.Count -gt 0) {
    foreach ($service in $exitedServices) {
        Write-Warning "$($service.name) exited during startup."
        $serviceLog = Join-Path $runDirectory "$($service.name).log"
        $serviceErrorLog = Join-Path $runDirectory "$($service.name).error.log"
        if (Test-Path -LiteralPath $serviceErrorLog) {
            Get-Content -LiteralPath $serviceErrorLog -Tail 30
        }
        if (Test-Path -LiteralPath $serviceLog) {
            Get-Content -LiteralPath $serviceLog -Tail 50
        }
    }
    exit 1
}

if ($waiting.Count -gt 0) {
    $failed = ($waiting | ForEach-Object { "$($_.Key):$($_.Value)" }) -join ", "
    Write-Warning "Not ready after 90 seconds: $failed"
    Write-Warning "Check .run\*.error.log and .run\*.log."
    exit 1
}

if ($SeedDemo) {
    Write-Host "Loading idempotent demo data..."
    $databaseUser = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) { "postgres_admin" } else { $env:POSTGRES_USER }
    $databaseName = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) { "mindcare_db" } else { $env:POSTGRES_DB }
    & docker exec mindcare_postgres psql -v ON_ERROR_STOP=1 -U $databaseUser -d $databaseName -f /opt/mindcare/seed-data.sql
    if ($LASTEXITCODE -ne 0) {
        throw "Could not load demo seed data. Check that every service completed its Flyway migrations."
    }
}

Write-Host ""
Write-Host "MindCare is ready:"
Write-Host "  Frontend:    http://localhost:5173"
Write-Host "  API Gateway: http://localhost:8079"
Write-Host "  pgAdmin:     http://localhost:5050"
Write-Host "Frontend API URL: $env:VITE_API_URL"
if ($realEmailEnabled) {
    Write-Host "Email mode:   Gmail SMTP ($env:SMTP_USERNAME)"
} else {
    Write-Host "Email mode:   Mailpit (http://localhost:8025)"
}
Write-Host "Stop application processes with: .\stop-all.ps1"
if (-not $SeedDemo) {
    Write-Host "Tip: run with -SeedDemo to load local demo accounts and sample activity."
}
