param(
    [string]$GatewayUrl = "http://localhost:8079",
    [string]$Email = "user1@mindcare.local",
    [string]$Password = "MindCare@123"
)

$ErrorActionPreference = "Stop"

function Invoke-DemoRequest {
    param(
        [string]$Name,
        [string]$Path,
        [hashtable]$Headers
    )

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$GatewayUrl$Path" -Headers $Headers -Method Get
        [PSCustomObject]@{
            Check = $Name
            Status = [int]$response.StatusCode
            Result = "OK"
        }
    }
    catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        throw "$Name failed with HTTP $status at $Path. $($_.Exception.Message)"
    }
}

$loginBody = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
}
catch {
    throw "Login failed at $GatewayUrl for $Email. $($_.Exception.Message)"
}

$token = if ($loginResponse.data.accessToken) { $loginResponse.data.accessToken } else { $loginResponse.accessToken }
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login response did not contain an access token."
}

$headers = @{ Authorization = "Bearer $token" }
$from = [Uri]::EscapeDataString((Get-Date).AddDays(-30).ToUniversalTime().ToString("o"))
$to = [Uri]::EscapeDataString((Get-Date).AddDays(1).ToUniversalTime().ToString("o"))
$timezone = [Uri]::EscapeDataString("Asia/Ho_Chi_Minh")

$checks = @(
    @{ Name = "Current user"; Path = "/api/auth/me" },
    @{ Name = "Emotion journals"; Path = "/api/v1/emotion-journals?from=$from&to=$to&limit=30" },
    @{ Name = "Emotion trends"; Path = "/api/v1/emotion-trends?from=$from&to=$to&bucket=DAY&timezone=$timezone" },
    @{ Name = "Assessments"; Path = "/api/v1/assessments" },
    @{ Name = "Assessment history"; Path = "/api/v1/assessment-results?from=$from&to=$to" },
    @{ Name = "Health benchmarks"; Path = "/api/v1/health-metrics/benchmark-evaluations" },
    @{ Name = "Care plan"; Path = "/api/v1/self-care-plan" },
    @{ Name = "Care recommendations"; Path = "/api/v1/self-care-plan/recommendations" },
    @{ Name = "AI conversations"; Path = "/api/ai/chat/conversations" },
    @{ Name = "Notifications"; Path = "/api/auth/notifications" }
)

$results = foreach ($check in $checks) {
    Invoke-DemoRequest -Name $check.Name -Path $check.Path -Headers $headers
}

$results | Format-Table -AutoSize
Write-Host "Demo smoke test passed: $($results.Count)/$($checks.Count) checks." -ForegroundColor Green
