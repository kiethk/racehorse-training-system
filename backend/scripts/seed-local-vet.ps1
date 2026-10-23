# seed-local-vet.ps1
# Seed du lieu bo sung nghiep vu Vet vao PostgreSQL Local (Docker rtms-postgres).
# Chi ghi phan cua Vet (health_records, care_schedule ROUTINE/URGENT, metrics,
# treatment_plans, prescriptions, injury_records). Idempotent - chay lai duoc.
# Su dung: .\scripts\seed-local-vet.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = $PSScriptRoot
$sqlFile = Join-Path $scriptDir 'seed-vet-demo.sql'

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Kiem tra Docker container rtms-postgres
Write-Host "[1/3] Kiem tra container 'rtms-postgres'..." -ForegroundColor Cyan
$container = docker ps --filter 'name=rtms-postgres' --format '{{.Names}}'
if (-not $container) {
    Write-Host "[ERROR] Container 'rtms-postgres' khong chay! Hay khoi dong: docker compose up -d rtms-db" -ForegroundColor Red
    exit 1
}

# 2. Thuc thi SQL seed (idempotent, chi ghi du lieu co marker [VET-DEMO])
Write-Host "[2/3] Dang nap du lieu Vet Demo vao Local PostgreSQL..." -ForegroundColor Yellow
# Dung redirect bytes tho qua cmd de UTF-8 khong bi PowerShell doi mau.
cmd /c "docker exec -i rtms-postgres psql -U rtms_user -d rtms_db -v ON_ERROR_STOP=1 -q < `"$sqlFile`""

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Nap du lieu seed that bai! Kiem tra thong bao loi o tren." -ForegroundColor Red
    exit 1
}

# 3. Kiem tra ket qua
Write-Host "[3/3] Kiem tra ket qua sau khi seed..." -ForegroundColor Cyan
$verify = docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "
SELECT concat('    - ', 'care_schedule: ', count(*)) FROM care_schedule WHERE description LIKE '[VET-DEMO]%'
UNION ALL SELECT concat('    - ', 'health_records: ', count(*)) FROM health_records WHERE notes LIKE '[VET-DEMO]%'
UNION ALL SELECT concat('    - ', 'horse_health_metrics: ', count(*)) FROM horse_health_metrics WHERE notes LIKE '[VET-DEMO]%'
UNION ALL SELECT concat('    - ', 'treatment_plans: ', count(*)) FROM treatment_plans WHERE instructions LIKE '[VET-DEMO]%'
UNION ALL SELECT concat('    - ', 'prescriptions: ', count(*)) FROM prescriptions WHERE instructions LIKE '[VET-DEMO]%'
UNION ALL SELECT concat('    - ', 'injury_records: ', count(*)) FROM injury_records WHERE description LIKE '[VET-DEMO]%';
"
$blocked = docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM horses WHERE training_decision = 'BLOCKED';"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " SEED VET DEMO LOCAL THANH CONG! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host $verify -ForegroundColor White
Write-Host "    - horses_BLOCKED: $($blocked.Trim())" -ForegroundColor White
Write-Host "  * Dang nhap kiem tra: vet1@rtms.com (admin123)" -ForegroundColor White
Write-Host "  * Idempotent: chay lai khong trung du lieu." -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
