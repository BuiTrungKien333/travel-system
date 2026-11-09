param([string]$Container = 'travel-postgres', [string]$Database = 'travel_db', [string]$User = 'postgres')
$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskSqlDirectory = Join-Path $taskRoot 'backend/src/main/resources/db/demo'
# Explicit DEV-only seed. All writes are guarded by the reserved demo UUID namespace.
foreach ($taskSqlName in @('seed_local_demo.sql', 'prepare_v1_demo.sql')) {
    Get-Content -LiteralPath (Join-Path $taskSqlDirectory $taskSqlName) -Raw -Encoding utf8 |
        & docker exec -i $Container psql -U $User -d $Database --single-transaction -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Demo preparation failed and that transaction was rolled back: $taskSqlName" }
}
Write-Host 'Demo ready: demo.local@example.com / DemoTravel!2026' -ForegroundColor Cyan
Write-Host 'Empty account: demo.user50@example.com / DemoTravel!2026'
