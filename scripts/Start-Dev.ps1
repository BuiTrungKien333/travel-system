param(
    [ValidateRange(1, 65535)][int]$Port = 18081,
    [switch]$WithoutAi,
    [switch]$UsePackagedJar,
    [uri]$WebUrl
)
$ErrorActionPreference = 'Stop'

if ($WebUrl -and (-not $WebUrl.IsAbsoluteUri -or $WebUrl.Scheme -notin @('http', 'https') -or
    $WebUrl.UserInfo -or $WebUrl.Query -or $WebUrl.Fragment -or $WebUrl.AbsolutePath -ne '/')) {
    throw 'WebUrl must be an HTTP(S) origin without credentials, path, query or fragment.'
}

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
if ($WebUrl) {
    $taskWebOrigin = $WebUrl.GetLeftPart([System.UriPartial]::Authority)
    $env:AUTH_VERIFY_EMAIL_URL = "$taskWebOrigin/verify-email"
    $env:AUTH_RESET_PASSWORD_URL = "$taskWebOrigin/reset-password"
    $env:GOOGLE_LINK_URL = "$taskWebOrigin/google-link"
    $taskOrigins = if ($env:CORS_ALLOWED_ORIGINS) { $env:CORS_ALLOWED_ORIGINS } else {
        'http://localhost:3000,http://localhost:8081,http://localhost:8082'
    }
    $env:CORS_ALLOWED_ORIGINS = (@($taskOrigins.Split(',').Trim()) + $taskWebOrigin | Select-Object -Unique) -join ','
    Write-Host "Dev email links and CORS include $taskWebOrigin."
}
& docker compose --env-file $taskRootEnv -f (Join-Path $taskRoot 'docker-compose.yml') up -d --build --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'Dev PostgreSQL with pgvector could not start.' }
if ($env:MONGODB_ENABLED -eq 'true') {
    & docker compose --env-file $taskRootEnv -f (Join-Path $taskRoot 'docker-compose.yml') up -d --wait mongodb
    if ($LASTEXITCODE -ne 0) { Write-Warning 'MongoDB is unavailable; starting core backend with PostgreSQL outbox buffering.' }
}

if ($WithoutAi) { $env:GEMINI_API_KEY = '' }
$env:SERVER_PORT = $Port.ToString()
Write-Host "Starting dev backend at http://localhost:$Port (database from the dev environment)."
Push-Location (Join-Path $taskRoot 'backend')
try {
    if ($UsePackagedJar) {
        $taskJar = Get-ChildItem -LiteralPath 'target' -Filter 'travel-backend-*.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if (-not $taskJar) { throw 'Build the backend first: .\mvnw.cmd -DskipTests package' }
        & java -Xmx768m -jar $taskJar.FullName '--spring.profiles.active=dev'
    } else {
        & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
    }
    if ($LASTEXITCODE -ne 0) { throw 'Dev backend failed.' }
} finally { Pop-Location }
