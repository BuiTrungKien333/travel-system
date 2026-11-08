param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
$source = Invoke-RestMethod -Uri "$($BaseUrl.TrimEnd('/'))/v3/api-docs"
$paths = [ordered]@{}
foreach ($entry in $source.paths.PSObject.Properties) {
    if ($entry.Name -match '^/api/v1/(media|journal/memories)(/|$)') { $paths[$entry.Name] = $entry.Value }
}
if ($paths.Count -eq 0) { throw 'No Memory/Media endpoints found' }
$schemas = [ordered]@{}
$pending = [System.Collections.Generic.Queue[string]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new()
function Add-SchemaReferences($value) {
    $serialized = ConvertTo-Json -InputObject $value -Depth 100 -Compress
    foreach ($match in [regex]::Matches($serialized, '#/components/schemas/([^"\\]+)')) {
        $name = $match.Groups[1].Value
        if ($seen.Add($name)) { $pending.Enqueue($name) }
    }
}
Add-SchemaReferences $paths
while ($pending.Count -gt 0) {
    $name = $pending.Dequeue()
    $schema = $source.components.schemas.PSObject.Properties[$name].Value
    if ($null -eq $schema) { throw "Missing schema reference: $name" }
    $schemas[$name] = $schema
    Add-SchemaReferences $schema
}
$document = [ordered]@{
    openapi = $source.openapi
    info = @{ title = 'Travel Memory and Media API'; version = '1.0.0'; description = 'Generated subset of Springdoc. Regenerate from the running backend with scripts/Export-MemoryApi.ps1.' }
    servers = @(@{ url = 'http://localhost:8080' })
    security = @(@{ bearerAuth = @() })
    paths = $paths
    components = @{ schemas = $schemas; securitySchemes = $source.components.securitySchemes }
}
$outputDirectory = Join-Path $PSScriptRoot '../docs/api'
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
$outputPath = Join-Path $outputDirectory 'memory-media.openapi.json'
[System.IO.File]::WriteAllText($outputPath, (ConvertTo-Json -InputObject $document -Depth 100) + "`n", [System.Text.UTF8Encoding]::new($false))
Write-Output "Exported $($paths.Count) paths and $($schemas.Count) schemas to $outputPath"
