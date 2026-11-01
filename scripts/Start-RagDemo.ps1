param([ValidateRange(1, 65535)][int]$Port = 18080)
$ErrorActionPreference = 'Stop'

$taskOccupied = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
    Where-Object { $_.Port -eq $Port }
if ($taskOccupied) {
    throw "Port $Port is already in use. Stop the old backend with Ctrl+C in its terminal, or run: .\scripts\Start-RagDemo.ps1 -Port 18081"
}

$taskRoot = Split-Path -Parent $PSScriptRoot
$taskEnvPath = Join-Path $taskRoot '.env.rag-demo'

# Dedicated, git-ignored demo credentials; never modify the existing .env.
if (-not (Test-Path -LiteralPath $taskEnvPath)) {
    "RAG_DEMO_DB_PASSWORD=$([guid]::NewGuid().ToString('N'))" | Set-Content -LiteralPath $taskEnvPath -Encoding utf8
}
$taskSettings = @{}
foreach ($taskLine in Get-Content -LiteralPath $taskEnvPath) {
    if ($taskLine -match '^([A-Z_]+)=(.*)$') { $taskSettings[$Matches[1]] = $Matches[2] }
}
foreach ($taskName in @('JWT_SECRET_KEY', 'JWT_REFRESH_KEY')) {
    if (-not $taskSettings.ContainsKey($taskName)) {
        $taskSettings[$taskName] = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
        "$taskName=$($taskSettings[$taskName])" | Add-Content -LiteralPath $taskEnvPath -Encoding utf8
    }
}
foreach ($taskName in $taskSettings.Keys) {
    [Environment]::SetEnvironmentVariable($taskName, $taskSettings[$taskName], 'Process')
}
$env:SERVER_PORT = $Port.ToString()
& docker compose --env-file $taskEnvPath -f (Join-Path $taskRoot 'docker-compose.rag-demo.yml') up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Demo PostgreSQL could not start.' }
Push-Location (Join-Path $taskRoot 'backend')
try {
    & mvn spring-boot:run '-Dspring-boot.run.profiles=rag-demo'

    if ($LASTEXITCODE -ne 0) { throw 'Demo backend failed.' }
} finally { Pop-Location }
