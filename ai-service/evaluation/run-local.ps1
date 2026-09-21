param(
    [string]$MavenPath = '',
    [string]$DatabaseUrl = 'jdbc:postgresql://localhost:5433/mindcare_db?options=-c%20default_transaction_read_only%3Don',
    [ValidateRange(0, 60000)][int]$CaseDelayMs = 3500
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $MavenPath) { $MavenPath = Join-Path $repoRoot 'auth-service/mvnw.cmd' }
$envFile = Join-Path $repoRoot '.env'
$names = @('GEMINI_API_KEY', 'GEMINI_EMBEDDING_MODEL', 'GEMINI_CHAT_MODEL', 'GEMINI_FALLBACK_CHAT_MODEL',
    'EVAL_DB_URL', 'EVAL_DB_USERNAME', 'EVAL_DB_PASSWORD', 'LIVE_RAG_EVAL', 'EVAL_CASE_DELAY_MS')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
            if ($line -match '^\s*(GEMINI_API_KEY|GEMINI_EMBEDDING_MODEL|GEMINI_CHAT_MODEL|GEMINI_FALLBACK_CHAT_MODEL)\s*=\s*(.*?)\s*$') {
                $name = $matches[1]
                $value = $matches[2].Trim().Trim('"').Trim("'")
                if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
                    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
                }
            }
        }
    }
    if (-not $env:GEMINI_API_KEY) { throw 'GEMINI_API_KEY is not configured.' }
    $env:EVAL_DB_URL = $DatabaseUrl
    if (-not $env:EVAL_DB_USERNAME) { $env:EVAL_DB_USERNAME = 'postgres_admin' }
    if (-not $env:EVAL_DB_PASSWORD) { $env:EVAL_DB_PASSWORD = 'secretpassword' }
    $env:LIVE_RAG_EVAL = 'true'
    $env:EVAL_CASE_DELAY_MS = $CaseDelayMs.ToString([Globalization.CultureInfo]::InvariantCulture)
    & $MavenPath -f (Join-Path $repoRoot 'ai-service/pom.xml') '-Dtest=RagEvaluationTest' '-Ddebug=false' test -q
    if ($LASTEXITCODE -ne 0) { throw "RAG evaluation failed (exit $LASTEXITCODE)." }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}
