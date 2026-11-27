$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running.'
}

$container = 'merchant-assistant-postgres'
$running = docker inspect --format '{{.State.Running}}' $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running -ne 'true') {
    throw 'The PostgreSQL container is not running. Start the project first.'
}

$backupDirectory = Join-Path $projectRoot 'data\backups'
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$containerFile = "/tmp/merchant-assistant-$timestamp.sql"
$backupFile = Join-Path $backupDirectory "merchant-assistant-$timestamp.sql"

docker exec $container pg_dump -U merchant -d merchant_assistant -f $containerFile
if ($LASTEXITCODE -ne 0) {
    throw 'PostgreSQL export failed.'
}

docker cp "${container}:$containerFile" $backupFile
if ($LASTEXITCODE -ne 0) {
    docker exec $container rm -f $containerFile | Out-Null
    throw 'Could not copy the backup into the project data folder.'
}

docker exec $container rm -f $containerFile | Out-Null
Write-Output "Database backup created: $backupFile"
