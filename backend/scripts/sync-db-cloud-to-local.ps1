# sync-db-cloud-to-local.ps1
# Dong bo toan bo database tu Neon Cloud ve Local Docker PostgreSQL (rtms-postgres)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))
$envCloudPath = Join-Path $backendRoot ".env.cloud"

if (-not (Test-Path $envCloudPath)) {
    Write-Host "[ERROR] Khong tim thay file .env.cloud tai $backendRoot" -ForegroundColor Red
    exit 1
}

# 1. Kiem tra Docker va container rtms-postgres
Write-Host "[1/4] Kiem tra Docker local..." -ForegroundColor Cyan
$container = docker ps --filter "name=rtms-postgres" --format "{{.Names}}"
if (-not $container) {
    Write-Host "[ERROR] Container 'rtms-postgres' khong chay! Hay chay: docker compose up -d" -ForegroundColor Red
    exit 1
}

# 2. Doc thong tin Neon Cloud tu .env.cloud
Write-Host "[2/4] Doc cau hinh Neon tu .env.cloud..." -ForegroundColor Cyan
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

# 3. Dump database tu Neon va nap vao Local Docker
Write-Host "[3/4] Dang dong bo du lieu tu Neon Cloud ve Docker Local (rtms-postgres)..." -ForegroundColor Yellow
$dumpArgs = @(
    "run", "--rm",
    "-e", "PGPASSWORD=$neonPassword",
    "postgres:18",
    "pg_dump",
    "-h", $neonHost,
    "-U", $neonUser,
    "-d", $neonDb,
    "--schema=public",
    "--clean",
    "--if-exists",
    "--no-owner",
    "--no-privileges"
)

# Chay pg_dump va pipe vao local psql
& docker @dumpArgs | docker exec -i rtms-postgres psql -U rtms_user -d rtms_db -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Dong bo that bai!" -ForegroundColor Red
    exit 1
}

# 4. Kiem tra ket qua sau dong bo
Write-Host "[4/4] Kiem tra du lieu sau khi dong bo..." -ForegroundColor Cyan
$horseCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM horses;").Trim()
$userCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM users;").Trim()
$areaCount = (docker exec rtms-postgres psql -U rtms_user -d rtms_db -t -c "SELECT count(*) FROM areas;").Trim()

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " DONG BO DU LIEU NEON -> LOCAL THANH CONG! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  * So luong Ngua:      $horseCount" -ForegroundColor White
Write-Host "  * So luong Nhan vien: $userCount" -ForegroundColor White
Write-Host "  * So luong Khu vuc:   $areaCount" -ForegroundColor White
Write-Host "  * Nguon anh:          Cloudinary (hien thi day du tren Local)" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
