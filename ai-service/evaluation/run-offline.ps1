$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

Push-Location (Join-Path $repoRoot 'ai-service')
try {
    & mvn.cmd '-Dtest=GeminiClientTest,GoldenSafetyTest,CrisisRiskDetectorTest,RagChatServiceTest,AiPrivacySecurityTest,AiConversationServiceTest' test
    if ($LASTEXITCODE -ne 0) { throw "Offline RAG Java gate failed (exit $LASTEXITCODE)." }
}
finally {
    Pop-Location
}

python -m unittest discover -s (Join-Path $repoRoot 'ai-service/evaluation') -p test_evaluation.py
if ($LASTEXITCODE -ne 0) { throw "RAG evaluation scorer tests failed (exit $LASTEXITCODE)." }

Write-Host 'Offline RAG safety and evaluation gates passed.' -ForegroundColor Green
