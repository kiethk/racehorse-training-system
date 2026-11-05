# seed-cloud-vet.ps1
# Seed du lieu bo sung nghiep vu Vet vao Neon Cloud.
# CHI ghi phan nghiep vu Vet, KHONG chay lai seed nen, KHONG upload file,
# KHONG doi gi du lieu nen (48 ngua, 54 admission, 324 file...).
# Idempotent: chi xoa/ghi du lieu co marker [VET-DEMO].
# Mat khau truyen qua environment (khong qua argv).
# Su dung: .\scripts\seed-cloud-vet.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir '..'))
$envCloudPath = Join-Path $backendRoot '.env.cloud'
$sqlFile = Join-Path $scriptDir 'seed-vet-demo.sql'

if (-not (Test-Path $envCloudPath)) {
    Write-Host "[ERROR] Khong tim thay file .env.cloud tai $backendRoot" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Doc cau hinh Neon tu .env.cloud (chi cho phep Neon SSL)
Write-Host "[1/3] Doc cau hinh Neon tu .env.cloud..." -ForegroundColor Cyan
$settings = @{}
foreach ($line in Get-Content $envCloudPath) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
        $parts = $trimmed.Split('=', 2)
        $settings[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
    }
}

$dbUrl = $settings['DB_URL']
$neonUser = $settings['DB_USERNAME']
$neonPassword = $settings['DB_PASSWORD']

if (-not $dbUrl -or -not $neonUser -or -not $neonPassword) {
    Write-Host "[ERROR] Thieu DB_URL, DB_USERNAME hoac DB_PASSWORD trong .env.cloud" -ForegroundColor Red
    exit 1
}

$rawUri = $dbUrl -replace '^jdbc:', ''
$uri = [System.Uri]$rawUri
if ($uri.Scheme -ne 'postgresql' -or -not $uri.Host.EndsWith('.neon.tech') -or $dbUrl -notmatch 'sslmode=require') {
    Write-Host "[ERROR] Chi duoc phep su dung Neon voi sslmode=require." -ForegroundColor Red
    exit 1
}

$neonHost = $uri.Host
$neonDb = $uri.AbsolutePath.TrimStart('/')

Write-Host "  -> Host: $neonHost" -ForegroundColor Gray
Write-Host "  -> Database: $neonDb" -ForegroundColor Gray
Write-Host "  -> User: $neonUser" -ForegroundColor Gray

# 2. Thuc thi SQL vao Neon qua container postgres:18 (mat khau qua env, khong argv)
Write-Host "[2/3] Dang nap du lieu Vet Demo vao Neon Cloud..." -ForegroundColor Yellow
$env:PGPASSWORD = $neonPassword
$env:PGSSLMODE = 'require'
try {
    # Redirect bytes tho qua cmd: UTF-8 khong bi PowerShell doi mau khi pipe.
    cmd /c "docker run --rm -i -e PGPASSWORD -e PGSSLMODE=require postgres:18 psql -h $neonHost -U $neonUser -d $neonDb -v ON_ERROR_STOP=1 -q < `"$sqlFile`""

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Nap du lieu seed vao Neon that bai!" -ForegroundColor Red
        exit 1
    }

    # 3. Kiem tra ket qua tren Neon
    Write-Host "[3/3] Kiem tra ket qua tren Neon Cloud..." -ForegroundColor Cyan

    function Run-NeonQuery([string]$sql) {
        $queryArgs = @(
            'run', '--rm',
            '-e', 'PGPASSWORD', '-e', 'PGSSLMODE=require',
            'postgres:18',
            'psql', '-h', $neonHost, '-U', $neonUser, '-d', $neonDb,
            '-t', '-c', $sql
        )
        $res = & docker @queryArgs
        return ($res -join "`n").Trim()
    }

    $care = Run-NeonQuery "SELECT count(*) FROM care_schedule WHERE description LIKE '[VET-DEMO]%';"
    $records = Run-NeonQuery "SELECT count(*) FROM health_records WHERE notes LIKE '[VET-DEMO]%';"
    $metrics = Run-NeonQuery "SELECT count(*) FROM horse_health_metrics WHERE notes LIKE '[VET-DEMO]%';"
    $plans = Run-NeonQuery "SELECT count(*) FROM treatment_plans WHERE instructions LIKE '[VET-DEMO]%';"
    $rx = Run-NeonQuery "SELECT count(*) FROM prescriptions WHERE instructions LIKE '[VET-DEMO]%';"
    $injuries = Run-NeonQuery "SELECT count(*) FROM injury_records WHERE description LIKE '[VET-DEMO]%';"
    $blocked = Run-NeonQuery "SELECT count(*) FROM horses WHERE training_decision = 'BLOCKED';"

    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " SEED VET DEMO NEON CLOUD THANH CONG! " -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "    - care_schedule: $care" -ForegroundColor White
    Write-Host "    - health_records: $records" -ForegroundColor White
    Write-Host "    - horse_health_metrics: $metrics" -ForegroundColor White
    Write-Host "    - treatment_plans: $plans" -ForegroundColor White
    Write-Host "    - prescriptions: $rx" -ForegroundColor White
    Write-Host "    - injury_records: $injuries" -ForegroundColor White
    Write-Host "    - horses_BLOCKED: $blocked" -ForegroundColor White
    Write-Host "==========================================================" -ForegroundColor Green
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PGSSLMODE -ErrorAction SilentlyContinue
}
