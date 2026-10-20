param(
    [Parameter(Mandatory = $true)] [string]$AssetsRoot,
    [ValidatePattern('^(rtms_db|rtms_seed_rehearsal_[0-9]+)$')] [string]$Database = 'rtms_db',
    [string]$NewGroomPassword = 'RtmsDemo@2026',
    [string]$NewOwnerPassword = 'RtmsDemo@2026',
    [switch]$VerifyOnly,
    [switch]$AddOwners
)
$ErrorActionPreference = 'Stop'
if ($VerifyOnly -and $AddOwners) { throw 'Choose either VerifyOnly or AddOwners.' }
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$assetPath = (Resolve-Path -LiteralPath $AssetsRoot).Path
if (-not (Test-Path -LiteralPath (Join-Path $assetPath 'seed-preview.json') -PathType Leaf)) {
    throw 'Missing seed-preview.json; supply the reviewed admission pack directory.'
}
$reportDirectory = Join-Path $backendRoot 'target\demo-seed'
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
$backupName = 'before-' + $Database + '-' + (Get-Date -Format 'yyyyMMddHHmmss') + '.dump'
$containerBackup = '/tmp/' + $backupName
$backupPath = Join-Path $reportDirectory $backupName

# 1. Backup local database if rtms-postgres is running
$runningContainer = cmd /c "docker ps --filter `"name=rtms-postgres`" --format `"{{.Names}}`""
if ($runningContainer -and $runningContainer.Trim() -eq 'rtms-postgres') {
    cmd /c "docker exec rtms-postgres pg_dump -h /var/run/postgresql -U rtms_user -d $Database --format=custom --file=$containerBackup > nul 2>&1"
    if ($LASTEXITCODE -eq 0) {
        cmd /c "docker cp `"rtms-postgres:$containerBackup`" `"$backupPath`" > nul 2>&1"
    }
}

if (-not $VerifyOnly -and -not $AddOwners) {
    # 2. Don sach thu muc uploads truoc khi seed de tranh trung lap UUID file
    $uploadDir = Join-Path $backendRoot 'uploads\admissions'
    if (Test-Path $uploadDir) {
        Write-Host "Cleaning uploads/admissions directory..." -ForegroundColor Cyan
        Get-ChildItem -Path $uploadDir -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue
    }

    # 3. Tu dong reset database Docker ve trang thai moi tinh de preflight pass
    $composeFile = Join-Path $backendRoot '..\docker-compose.yml'
    if (Test-Path $composeFile) {
        Write-Host "Resetting local Postgres container via docker-compose..." -ForegroundColor Cyan
        cmd /c "docker compose -f `"$composeFile`" down -v > nul 2>&1"
        cmd /c "docker compose -f `"$composeFile`" up -d > nul 2>&1"
        
        Write-Host "Waiting for Postgres to accept connections..." -ForegroundColor Cyan
        $retries = 30
        while ($retries -gt 0) {
            cmd /c "docker exec rtms-postgres pg_isready -U rtms_user -d $Database > nul 2>&1"
            if ($LASTEXITCODE -eq 0) { break }
            Start-Sleep -Seconds 1
            $retries--
        }
        Start-Sleep -Milliseconds 500
    }
}

$previousPassword = $env:RTMS_SEED_PASSWORD
$previousDatabase = $env:RTMS_DEMO_SEED_DATABASE
$previousOwnerPassword = $env:RTMS_OWNER_SEED_PASSWORD
try {
    $env:RTMS_SEED_PASSWORD = $NewGroomPassword
    $env:RTMS_OWNER_SEED_PASSWORD = $NewOwnerPassword
    $env:RTMS_DEMO_SEED_DATABASE = $Database
    Push-Location $backendRoot
    try {
        $seedArguments = @('test', '-Dtest=LocalDemoSeedToolTest', '-Drtms.seed.local=true', "-Drtms.seed.assetsRoot=$assetPath")
        if ($VerifyOnly) { $seedArguments += '-Drtms.seed.verifyOnly=true' }
        if ($AddOwners) { $seedArguments += '-Drtms.seed.addOwners=true' }
        $runLogPath = Join-Path $reportDirectory 'run.log'
        $argString = $seedArguments -join ' '
        Write-Host "Running Maven seed workflow (this may take 1-2 minutes)..." -ForegroundColor Cyan
        cmd /c ".\mvnw.cmd $argString > `"$runLogPath`" 2>&1"
        if ($LASTEXITCODE -ne 0) {
            Get-Content -LiteralPath $runLogPath -Tail 65
            throw "Seed stopped. Local backup: $backupPath. Check run.log before retrying; partial commits may exist."
        }
    } finally { Pop-Location }
    Get-Content -LiteralPath (Join-Path $reportDirectory 'result.json')
    Write-Output "Local backup: $backupPath"
} finally {
    $env:RTMS_SEED_PASSWORD = $previousPassword
    $env:RTMS_DEMO_SEED_DATABASE = $previousDatabase
    $env:RTMS_OWNER_SEED_PASSWORD = $previousOwnerPassword
}
