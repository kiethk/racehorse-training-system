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
# Explicit Docker socket/local account only. Never reads .env.cloud or run-cloud.ps1.
docker exec rtms-postgres pg_dump -h /var/run/postgresql -U rtms_user -d $Database --format=custom --file=$containerBackup
if ($LASTEXITCODE -ne 0) { throw 'Local backup failed; seed not started.' }
docker cp "rtms-postgres:$containerBackup" $backupPath
if ($LASTEXITCODE -ne 0) { throw 'Could not save backup; seed not started.' }
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
        & .\mvnw.cmd @seedArguments *> (Join-Path $reportDirectory 'run.log')
        if ($LASTEXITCODE -ne 0) {
            Get-Content -LiteralPath (Join-Path $reportDirectory 'run.log') -Tail 65
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
