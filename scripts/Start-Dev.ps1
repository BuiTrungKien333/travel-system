param([ValidateRange(1, 65535)][int]$Port = 18081, [switch]$WithoutAi)
$ErrorActionPreference = 'Stop'

$taskRoot = Split-Path -Parent $PSScriptRoot
$taskOccupied = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
    Where-Object { $_.Port -eq $Port }
if ($taskOccupied) {
    throw "Port $Port is already in use. Select another port: .\scripts\Start-Dev.ps1 -Port 18082"
}

# Docker and Spring share the same environment; backend-local values override root defaults.
$taskRootEnv = Join-Path $taskRoot '.env'
if (-not (Test-Path -LiteralPath $taskRootEnv)) {
    throw 'Create the root .env from .env.example before starting dev.'
}
$taskBackendEnv = Join-Path $taskRoot 'backend/.env'
foreach ($taskEnvPath in @($taskRootEnv, $taskBackendEnv)) {
    if (Test-Path -LiteralPath $taskEnvPath) {
        foreach ($taskLine in Get-Content -LiteralPath $taskEnvPath) {
            if ($taskLine -match '^([A-Z_]+)=(.*)$') {
                [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
            }
        }
    }
}
& docker compose --env-file $taskRootEnv -f (Join-Path $taskRoot 'docker-compose.yml') up -d --build --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'Dev PostgreSQL with pgvector could not start.' }
if ($WithoutAi) { $env:GEMINI_API_KEY = '' }
$env:SERVER_PORT = $Port.ToString()
Write-Host "Starting dev backend at http://localhost:$Port (database from the dev environment)."
Push-Location (Join-Path $taskRoot 'backend')
try {
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
    if ($LASTEXITCODE -ne 0) { throw 'Dev backend failed.' }
} finally { Pop-Location }
