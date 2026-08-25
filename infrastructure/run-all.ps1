[CmdletBinding()]
param(
    [switch]$SeedDemo,
    [string]$GeminiApiKey = $env:GEMINI_API_KEY
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$composeFile = Join-Path $PSScriptRoot 'docker-compose.yml'
$jwtSecret = 'TWluZENhcmUtRGV2ZWxvcG1lbnQtSldULVNlY3JldC0yMDI2IQ=='

if ([string]::IsNullOrWhiteSpace($GeminiApiKey)) {
    throw 'Thiếu GEMINI_API_KEY. Chạy: $env:GEMINI_API_KEY="..." rồi gọi lại script.'
}

function Wait-TcpPort {
    param([int]$Port, [string]$Name, [int]$TimeoutSeconds = 120)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue) {
            Write-Host "[OK] $Name is listening on port $Port" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "$Name did not start on port $Port within $TimeoutSeconds seconds. Check its terminal log."
}

function Start-MindCareService {
    param([string]$Title, [string]$Command)
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes(
        "`$Host.UI.RawUI.WindowTitle='$Title'; Set-Location '$repoRoot'; $Command"))
    Start-Process powershell.exe -ArgumentList '-NoExit', '-EncodedCommand', $encoded
}

Write-Host '[1/6] Starting PostgreSQL, pgAdmin and Mailpit...'
docker compose -f $composeFile up -d
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed.' }
Wait-TcpPort -Port 5433 -Name 'PostgreSQL'

Write-Host '[2/6] Starting Auth Service...'
Start-MindCareService -Title 'MindCare - Auth :8081' -Command @"
`$env:JWT_SECRET='$jwtSecret';
`$env:SMTP_HOST='localhost'; `$env:SMTP_PORT='1025'; `$env:FRONTEND_URL='http://localhost:5173';
& '.\auth-service\mvnw.cmd' -f 'auth-service\pom.xml' spring-boot:run
"@

Write-Host '[3/6] Starting Emotion Service...'
Start-MindCareService -Title 'MindCare - Emotion :8083' -Command @"
`$env:SEED_ASSESSMENTS='true';
& '.\emotion-service\mvnw.cmd' -f 'emotion-service\pom.xml' spring-boot:run
"@

Write-Host '[4/6] Starting AI Service...'
$escapedGeminiKey = $GeminiApiKey.Replace("'", "''")
Start-MindCareService -Title 'MindCare - AI :8084' -Command @"
`$env:GEMINI_API_KEY='$escapedGeminiKey';
& '.\auth-service\mvnw.cmd' -f 'ai-service\pom.xml' spring-boot:run
"@

Wait-TcpPort -Port 8081 -Name 'Auth Service'
Wait-TcpPort -Port 8083 -Name 'Emotion Service'
Wait-TcpPort -Port 8084 -Name 'AI Service'

if ($SeedDemo) {
    Write-Host '[seed] Loading idempotent demo data...'
    docker exec mindcare_postgres psql -v ON_ERROR_STOP=1 -U postgres_admin -d mindcare_db -f /opt/mindcare/seed-data.sql
    if ($LASTEXITCODE -ne 0) { throw 'Demo seed failed.' }
}

Write-Host '[5/6] Starting API Gateway...'
Start-MindCareService -Title 'MindCare - Gateway :8079' -Command @"
& '.\api-gateway\mvnw.cmd' -f 'api-gateway\pom.xml' spring-boot:run
"@
Wait-TcpPort -Port 8079 -Name 'API Gateway'

Write-Host '[6/6] Starting frontend...'
Start-MindCareService -Title 'MindCare - Frontend :5173' -Command @"
Set-Location '.\mindcare-frontend\web-app';
if (-not (Test-Path 'node_modules')) { npm install };
npm run dev
"@

Write-Host ''
Write-Host 'MindCare startup completed.' -ForegroundColor Cyan
Write-Host 'Frontend : http://localhost:5173'
Write-Host 'Gateway  : http://localhost:8079'
Write-Host 'Mailpit  : http://localhost:8025'
Write-Host 'pgAdmin  : http://localhost:5050'
