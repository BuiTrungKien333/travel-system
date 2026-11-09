param(
    [ValidateRange(1024, 65535)][int]$Port = 18081,
    [ValidateRange(1024, 65535)][int]$WebPort = 8082,
    [string]$LanIp,
    [string]$MobileRoot = 'E:\travel-mobile-ui',
    [switch]$WithoutWeb
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
if (-not $LanIp) {
    $taskNetworks = @(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' -and $_.InterfaceAlias -notmatch 'vEthernet|Docker|WSL' })
    $taskNetwork = $taskNetworks | Sort-Object @{ Expression = { if ($_.InterfaceAlias -match 'Wi-Fi|Wireless') { 0 } else { 1 } } } | Select-Object -First 1
    if ($taskNetwork) { $LanIp = $taskNetwork.IPv4Address.IPAddress | Select-Object -First 1 }
}
$taskAddress = $null
if (-not [Net.IPAddress]::TryParse($LanIp, [ref]$taskAddress) -or $taskAddress.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) {
    throw 'No LAN IPv4 found. Connect Wi-Fi or pass -LanIp 192.168.x.x.'
}
$taskApiUrl = "http://${LanIp}:$Port"
$taskWebUrl = "http://${LanIp}:$WebPort"
Write-Host "`nWeGo API: $taskApiUrl" -ForegroundColor Cyan
Write-Host "APK: Login -> Ket noi may chu -> $taskApiUrl"
Write-Host "Web fallback: http://localhost:$WebPort (or $taskWebUrl)`n"

$taskBackendListener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($taskBackendListener) {
    $taskRunning = Get-CimInstance Win32_Process -Filter "ProcessId = $($taskBackendListener.OwningProcess)"
    $taskBackendPath = (Join-Path $taskRoot 'backend').Replace('\', '/')
    $taskCommand = ([string]$taskRunning.CommandLine).Replace('\', '/')
    $taskIsWeGo = $taskCommand.IndexOf($taskBackendPath, [StringComparison]::OrdinalIgnoreCase) -ge 0 -or $taskCommand.Contains('com.travel.TravelApplication')
    if ($taskRunning.Name -notmatch '^java(w)?\.exe$' -or -not $taskIsWeGo) {
        throw "Port $Port belongs to another process. Close it yourself or choose -Port."
    }
    Write-Host 'Restarting the existing WeGo backend with the current Wi-Fi address.'
    Stop-Process -Id $taskBackendListener.OwningProcess
    Start-Sleep -Milliseconds 500
}

if (-not $WithoutWeb) {
    $taskWebBuild = Join-Path $MobileRoot '.expo/v1-web-release'
    if (-not (Test-Path -LiteralPath $taskWebBuild)) { throw "Web export missing: $taskWebBuild" }
    $taskWebListener = Get-NetTCPConnection -LocalPort $WebPort -State Listen -ErrorAction SilentlyContinue
    if (-not $taskWebListener) {
        $taskLogDir = Join-Path $MobileRoot '.expo/v1-release'
        New-Item -ItemType Directory -Path $taskLogDir -Force | Out-Null
        Start-Process -FilePath (Get-Command node).Source -ArgumentList @('node_modules/expo/bin/cli', 'serve', '.expo/v1-web-release', '--port', $WebPort) -WorkingDirectory $MobileRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $taskLogDir 'web.stdout.log') -RedirectStandardError (Join-Path $taskLogDir 'web.stderr.log') | Out-Null
    } else { Write-Host "Port $WebPort already has a server; leaving it running." }
}

# Start-Dev loads the existing local secrets privately and adds the new Wi-Fi origin to CORS/email links.
& (Join-Path $PSScriptRoot 'Start-Dev.ps1') -Port $Port -WithoutAi -UsePackagedJar -WebUrl $taskWebUrl
