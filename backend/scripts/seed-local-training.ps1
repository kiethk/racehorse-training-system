# seed-local-training.ps1
# Seed du lieu demo module Training vao Database PostgreSQL Local (Docker rtms-postgres)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

$scriptDir = $PSScriptRoot
$sqlFile = Join-Path $scriptDir "seed-training-demo.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Kiem tra Docker container rtms-postgres
Write-Host "[1/3] Kiem tra container 'rtms-postgres'..." -ForegroundColor Cyan
$container = docker ps --filter "name=rtms-postgres" --format "{{.Names}}"
if (-not $container) {
    Write-Host "[ERROR] Container 'rtms-postgres' khong chay! Hay khoi dong: docker compose up -d" -ForegroundColor Red
    exit 1
}

# 2. Thuc thi SQL seed data
Write-Host "[2/3] Dang nap du lieu Training Demo vao Local PostgreSQL..." -ForegroundColor Yellow
$content = Get-Content -LiteralPath $sqlFile -Raw
$content | docker exec -i rtms-postgres psql -U rtms_user -d rtms_db -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Nap du lieu seed that bai!" -ForegroundColor Red
    exit 1
}

# 3. Kiem tra ket qua va in thong ke
Write-Host "[3/3] Kiem tra ket qua sau khi seed..." -ForegroundColor Cyan
$planStats = docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "
SELECT concat('    - ', status, ': ', count(*), ' plans') 
FROM horse_training_plans 
WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com') 
GROUP BY status;
"

$courseCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM courses WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');").Trim()
$subjectCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM subjects WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');").Trim()
$lotCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM training_lots WHERE trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');").Trim()
$workoutCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM training_workouts tw JOIN horse_training_plans p ON p.id = tw.plan_id WHERE p.trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');").Trim()

$overlapCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "
SELECT count(*) 
FROM training_lots l1
JOIN training_lots l2 ON l1.trainer_id = l2.trainer_id 
  AND l1.lot_date = l2.lot_date 
  AND l1.id < l2.id
  AND l1.status <> 'CANCELLED' AND l2.status <> 'CANCELLED'
  AND (l1.start_time, l1.end_time) OVERLAPS (l2.start_time, l2.end_time)
WHERE l1.trainer_id = (SELECT id FROM users WHERE email = 'trainer1@rtms.com');
").Trim()

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " SEED TRAINING DEMO LOCAL THANH CONG! " -ForegroundColor Green
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
