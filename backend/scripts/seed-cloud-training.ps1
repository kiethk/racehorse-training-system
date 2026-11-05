# seed-cloud-training.ps1
# Seed du lieu demo module Training truc tiep vao Database Neon Cloud

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))
$envCloudPath = Join-Path $backendRoot ".env.cloud"
$sqlFile = Join-Path $scriptDir "seed-training-demo.sql"

if (-not (Test-Path $envCloudPath)) {
    Write-Host "[ERROR] Khong tim thay file .env.cloud tai $backendRoot" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Doc thong tin Neon Cloud tu .env.cloud
Write-Host "[1/3] Doc cau hinh Neon tu .env.cloud..." -ForegroundColor Cyan
$settings = @{}
foreach ($line in Get-Content $envCloudPath) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
        $parts = $trimmed.Split("=", 2)
        $settings[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
    }
}

$dbUrl = $settings["DB_URL"]
$neonUser = $settings["DB_USERNAME"]
$neonPassword = $settings["DB_PASSWORD"]

if (-not $dbUrl -or -not $neonUser -or -not $neonPassword) {
    Write-Host "[ERROR] Thieu DB_URL, DB_USERNAME hoac DB_PASSWORD trong .env.cloud" -ForegroundColor Red
    exit 1
}

$rawUri = $dbUrl -replace '^jdbc:', ''
$uri = [System.Uri]$rawUri
$neonHost = $uri.Host
$neonDb = $uri.AbsolutePath.TrimStart('/')

Write-Host "  -> Host: $neonHost" -ForegroundColor Gray
Write-Host "  -> Database: $neonDb" -ForegroundColor Gray
Write-Host "  -> User: $neonUser" -ForegroundColor Gray

# 2. Thuc thi SQL seed data vao Neon Cloud qua Docker container postgres:18
Write-Host "[2/3] Dang nap du lieu Training Demo vao Neon Cloud..." -ForegroundColor Yellow
$content = Get-Content -LiteralPath $sqlFile -Raw

$psqlArgs = @(
    "run", "--rm", "-i",
    "-e", "PGPASSWORD=$neonPassword",
    "postgres:18",
    "psql",
    "-h", $neonHost,
    "-U", $neonUser,
    "-d", $neonDb,
    "-q"
)

$content | docker @psqlArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Nap du lieu seed vao Neon Cloud that bai!" -ForegroundColor Red
    exit 1
}

# 3. Kiem tra ket qua sau khi seed tren Neon
Write-Host "[3/3] Kiem tra ket qua tren Neon Cloud..." -ForegroundColor Cyan

function Run-NeonQuery([string]$sql) {
    $queryArgs = @(
        "run", "--rm",
        "-e", "PGPASSWORD=$neonPassword",
        "postgres:18",
        "psql",
        "-h", $neonHost,
        "-U", $neonUser,
        "-d", $neonDb,
        "-t", "-c", $sql
    )
    $res = & docker @queryArgs
    return ($res -join "`n").Trim()
}

$planStats = Run-NeonQuery "
SELECT concat('    - ', status, ': ', count(*), ' plans') 
FROM horse_training_plans 
WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com') 
GROUP BY status;
"

$courseCount = Run-NeonQuery "SELECT count(*) FROM courses WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');"
$subjectCount = Run-NeonQuery "SELECT count(*) FROM subjects WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');"
$lotCount = Run-NeonQuery "SELECT count(*) FROM training_lots WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');"
$workoutCount = Run-NeonQuery "SELECT count(*) FROM training_workouts tw JOIN horse_training_plans p ON p.id = tw.plan_id WHERE p.trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');"

$overlapCount = Run-NeonQuery "
SELECT count(*) 
FROM training_lots l1
JOIN training_lots l2 ON l1.trainer_id = l2.trainer_id 
  AND l1.lot_date = l2.lot_date 
  AND l1.id < l2.id
  AND l1.status <> 'CANCELLED' AND l2.status <> 'CANCELLED'
  AND (l1.start_time, l1.end_time) OVERLAPS (l2.start_time, l2.end_time)
WHERE l1.trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');
"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " SEED TRAINING DEMO NEON CLOUD THANH CONG! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  * Tai khoan test chinh: trainer1@rtms.com (Michael Turner)" -ForegroundColor White
Write-Host "  * So luong Subjects:    $subjectCount (10 mon hoc chuyen sau)" -ForegroundColor White
Write-Host "  * So luong Courses:     $courseCount (6 khoa hoc)" -ForegroundColor White
Write-Host "  * So luong Lots:        $lotCount (khung gio 06:00 - 10:00)" -ForegroundColor White
Write-Host "  * So luong Workouts:    $workoutCount buoi tap" -ForegroundColor White
Write-Host "  * Kiem tra trung Lot:   $overlapCount overlap (Hoan toan khong trung!)" -ForegroundColor White
Write-Host "  * Chi tiet Plans:" -ForegroundColor White
Write-Host $planStats -ForegroundColor Gray
Write-Host "  * Ngua chua co Plan:    2 con (B08 & B09 de test ghi danh)" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
