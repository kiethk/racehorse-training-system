param(
    [Parameter(Mandatory=$true)] [string]$AssetsRoot,
    [ValidateSet('Preflight','Seed','Verify')] [string]$Mode='Preflight',
    [string]$NewAccountPassword='RtmsDemo@2026'
)
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$assets=(Resolve-Path -LiteralPath $AssetsRoot).Path
if (-not (Test-Path -LiteralPath (Join-Path $assets 'seed-preview.json'))) { throw 'Reviewed assets missing.' }
$settings=@{}
foreach ($line in Get-Content -LiteralPath (Join-Path $backendRoot '.env.cloud')) {
    $entry=$line.Trim()
    if ($entry -and -not $entry.StartsWith('#') -and $entry.Contains('=')) {
        $parts=$entry.Split('=',2); $settings[$parts[0].Trim()]=$parts[1].Trim().Trim('"').Trim("'")
    }
}
foreach ($key in @('DB_URL','DB_USERNAME','DB_PASSWORD','CLOUDINARY_URL')) {
    if (-not $settings[$key]) { throw "Missing required setting: $key" }
}
$uri=[Uri]($settings['DB_URL'] -replace '^jdbc:','')
if ($uri.Scheme -ne 'postgresql' -or -not $uri.Host.EndsWith('.neon.tech') -or $uri.Query -notmatch 'sslmode=require') { throw 'Only reviewed SSL Neon configuration is allowed.' }
if (-not $settings['CLOUDINARY_URL'].StartsWith('cloudinary://')) { throw 'Cloudinary configuration required.' }
$report=Join-Path $backendRoot 'target/cloud-seed'
$backups=Join-Path $report 'backups'
New-Item -ItemType Directory -Path $backups -Force | Out-Null
$activeFile=Join-Path $report 'operation.running'
$lock=[IO.File]::Open($activeFile,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
$environment=@{
    DB_URL=$settings['DB_URL']; DB_USERNAME=$settings['DB_USERNAME']; DB_PASSWORD=$settings['DB_PASSWORD']; CLOUDINARY_URL=$settings['CLOUDINARY_URL'];
    RTMS_SEED_CLOUD_TARGET=$settings['DB_URL']; RTMS_SEED_PASSWORD=$NewAccountPassword; RTMS_OWNER_SEED_PASSWORD=$NewAccountPassword;
    PGHOST=$uri.Host; PGPORT=$(if($uri.Port -gt 0){[string]$uri.Port}else{'5432'}); PGDATABASE=$uri.AbsolutePath.TrimStart('/');
    PGUSER=$settings['DB_USERNAME']; PGPASSWORD=$settings['DB_PASSWORD']; PGSSLMODE='require'; PGCONNECT_TIMEOUT='15'; PGOPTIONS='';
    RTMS_SEED_BACKUP=''; RTMS_SEED_BACKUP_SHA256=''
}
$previous=@{}
try {
    foreach ($key in $environment.Keys) { $previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process'); [Environment]::SetEnvironmentVariable($key,$environment[$key],'Process') }
    # Credentials travel in inherited environment variables, never command-line argument values.
    $name='before-neon-'+(Get-Date -Format 'yyyyMMddHHmmss')+'.dump'
    $containerFile='/backup/'+$name
    $backup=Join-Path $backups $name
    $dockerEnvironment=@('run','--rm')
    foreach ($key in @('PGHOST','PGPORT','PGDATABASE','PGUSER','PGPASSWORD','PGSSLMODE','PGCONNECT_TIMEOUT','PGOPTIONS')) { $dockerEnvironment+=@('--env',$key) }
    $backupMount="type=bind,source=$backups,target=/backup"
    $oldEAP = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker @dockerEnvironment --mount $backupMount postgres:18 pg_dump -w --format=custom "--file=$containerFile" *> (Join-Path $report 'backup.log')
        if ($LASTEXITCODE -ne 0) { throw 'Cloud backup failed; database has not been seeded. Inspect private target/cloud-seed/backup.log.' }
        if (-not (Test-Path -LiteralPath $backup)) { throw 'Cloud backup missing; seed not started.' }
        & docker run --rm --mount $backupMount postgres:18 pg_restore --list $containerFile *> (Join-Path $report 'backup-contents.txt')
        if ($LASTEXITCODE -ne 0) { throw 'Cloud backup validation failed; seed not started.' }
    } finally {
        $ErrorActionPreference = $oldEAP
    }
    $env:RTMS_SEED_BACKUP=$backup
    $env:RTMS_SEED_BACKUP_SHA256=(Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash.ToLowerInvariant()
    Push-Location $backendRoot
    try {
        $arguments=@('test','-Dtest=CloudDemoSeedToolTest','-Drtms.seed.cloud=true',"-Drtms.seed.assetsRoot=$assets")
        if ($Mode -eq 'Preflight') { $arguments+='-Drtms.seed.cloud.preflightOnly=true' }
        if ($Mode -eq 'Verify') { $arguments+='-Drtms.seed.verifyOnly=true' }
        $runLog = Join-Path $report ($Mode.ToLowerInvariant()+'.log')
        $argString = $arguments -join ' '
        cmd /c ".\mvnw.cmd $argString > `"$runLog`" 2>&1"
        if ($LASTEXITCODE -ne 0) { throw "Cloud $Mode stopped. Inspect private target/cloud-seed logs and backup before retrying; never blindly rerun a partial seed." }
    } finally { Pop-Location }
    Write-Output "Cloud $Mode PASS. Backup saved. No credentials printed."
} finally {
    # JDBC URLs can embed credentials; sanitize private logs too before surfacing any failure.
    # The live application may be writing server.log; only sanitize completed seed logs.
    $logs=@(Get-ChildItem -LiteralPath $report -Filter '*.log' -File | Where-Object { $_.Name -in @('preflight.log','seed.log','verify.log','backup.log','compile.log') })
    $surefire=Join-Path $backendRoot 'target/surefire-reports'
    if (Test-Path -LiteralPath $surefire) { $logs+=@(Get-ChildItem -LiteralPath $surefire -Filter '*CloudDemoSeedToolTest*' -File) }
    try {
        foreach ($file in $logs) {
            $content=[IO.File]::ReadAllText($file.FullName)
            foreach ($key in @('DB_URL','DB_PASSWORD','CLOUDINARY_URL')) {
                if ($settings[$key]) { $content=$content.Replace($settings[$key],'[redacted]') }
            }
            [IO.File]::WriteAllText($file.FullName,$content)
        }
    } finally {
        foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key,$previous[$key],'Process') }
        $lock.Dispose()
        Remove-Item -LiteralPath $activeFile
    }
}
