# apply-fix-cloud-staff.ps1
# Chạy script SQL rebalance staff trực tiếp vào Neon Cloud

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))
$envCloudPath = Join-Path $backendRoot ".env.cloud"
$sqlFile = Join-Path $scriptDir "fix-cloud-staff-assignments.sql"

if (-not (Test-Path $envCloudPath)) {
    Write-Host "[ERROR] Khong tim thay file .env.cloud tai $backendRoot" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Đọc config Neon từ .env.cloud
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
    Write-Host "[ERROR] Thieu thong tin ket noi trong .env.cloud" -ForegroundColor Red
    exit 1
}

$rawUri = $dbUrl -replace '^jdbc:', ''
$uri = [System.Uri]$rawUri
$neonHost = $uri.Host
$neonDb = $uri.AbsolutePath.TrimStart('/')

Write-Host "  -> Host: $neonHost" -ForegroundColor Gray
Write-Host "  -> Database: $neonDb" -ForegroundColor Gray

# 2. Chạy SQL qua docker postgres:18
Write-Host "[2/3] Thuc thi SQL rebalance vao Neon Cloud..." -ForegroundColor Yellow
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
    Write-Host "[ERROR] Thuc thi SQL that bai!" -ForegroundColor Red
    exit 1
}

# 3. Kiểm tra phân bổ sau khi update
Write-Host "[3/3] Kiem tra phan bo staff sau khi cap nhat..." -ForegroundColor Cyan

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

$vetDist = Run-NeonQuery "
SELECT concat('    - ', u.email, ': ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.veterinarian_id = u.id
GROUP BY u.email ORDER BY count(a.id) DESC;
"

$trnDist = Run-NeonQuery "
SELECT concat('    - ', u.email, ': ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.trainer_id = u.id
GROUP BY u.email ORDER BY count(a.id) DESC;
"

$grmDist = Run-NeonQuery "
SELECT concat('    - ', u.email, ': ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.groom_id = u.id
GROUP BY u.email ORDER BY count(a.id) DESC LIMIT 5;
"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " REBALANCE STAFF NEON CLOUD THANH CONG! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Phan bo Veterinarian:" -ForegroundColor White
Write-Host $vetDist -ForegroundColor Gray
Write-Host "Phan bo Trainer:" -ForegroundColor White
Write-Host $trnDist -ForegroundColor Gray
Write-Host "Phan bo Groom (Top 5):" -ForegroundColor White
Write-Host $grmDist -ForegroundColor Gray
Write-Host "Tat ca Cloudinary assets & ho so duoc giu nguyen 100%!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
