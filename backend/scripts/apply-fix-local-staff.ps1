# apply-fix-local-staff.ps1
# Chạy script SQL rebalance staff trực tiếp vào Native Local PostgreSQL (rtms_db)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))
$localPropsPath = Join-Path $backendRoot "src\main\resources\application-local.properties"
$sqlFile = Join-Path $scriptDir "fix-cloud-staff-assignments.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] Khong tim thay file SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# 1. Đọc config từ application-local.properties
Write-Host "[1/3] Doc cau hinh tu application-local.properties..." -ForegroundColor Cyan

$dbHost = "localhost"
$dbPort = "5434"
$dbName = "rtms_db"
$dbUser = "postgres"
$dbPassword = ""

if (Test-Path $localPropsPath) {
    foreach ($line in Get-Content $localPropsPath) {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
            $parts = $trimmed.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim().Trim('"').Trim("'")
            if ($key -eq "spring.datasource.url") {
                # jdbc:postgresql://localhost:5434/rtms_db
                $cleanUrl = $val -replace '^jdbc:', ''
                try {
                    $uri = [System.Uri]$cleanUrl
                    $dbHost = $uri.Host
                    if ($uri.Port -gt 0) { $dbPort = $uri.Port.ToString() }
                    $dbName = $uri.AbsolutePath.TrimStart('/')
                } catch {
                    Write-Host "  -> Parse URL error, fallback to regex" -ForegroundColor Yellow
                    if ($val -match "://([^:/]+):?(\d+)?/(.+)$") {
                        $dbHost = $Matches[1]
                        if ($Matches[2]) { $dbPort = $Matches[2] }
                        $dbName = $Matches[3]
                    }
                }
            } elseif ($key -eq "spring.datasource.username") {
                $dbUser = $val
            } elseif ($key -eq "spring.datasource.password") {
                $dbPassword = $val
            }
        }
    }
} else {
    Write-Host "  -> File application-local.properties khong ton tai, dung cau hinh mac dinh." -ForegroundColor Yellow
}

Write-Host "  -> Host: $dbHost" -ForegroundColor Gray
Write-Host "  -> Port: $dbPort" -ForegroundColor Gray
Write-Host "  -> Database: $dbName" -ForegroundColor Gray
Write-Host "  -> User: $dbUser" -ForegroundColor Gray

# 2. Tìm psql.exe
$psqlPath = $null
$psqlCmd = Get-Command psql.exe -ErrorAction SilentlyContinue
if ($psqlCmd) {
    $psqlPath = $psqlCmd.Source
} else {
    $candidates = @(Get-ChildItem -Path "C:\Program Files\PostgreSQL" -Filter "psql.exe" -Recurse -ErrorAction SilentlyContinue)
    if ($candidates.Count -gt 0) {
        $psqlPath = $candidates[0].FullName
    }
}

if (-not $psqlPath) {
    Write-Host "[ERROR] Khong tim thay psql.exe tren he thong!" -ForegroundColor Red
    exit 1
}

Write-Host "  -> Dung psql tai: $psqlPath" -ForegroundColor Gray

# 3. Chạy SQL rebalance
Write-Host "[2/3] Thuc thi SQL rebalance vao Local PostgreSQL..." -ForegroundColor Yellow
$prevPgPass = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $dbPassword
    & $psqlPath -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $sqlFile -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Thuc thi SQL that bai!" -ForegroundColor Red
        exit 1
    }
} finally {
    $env:PGPASSWORD = $prevPgPass
}

# 4. Kiểm tra phân bổ sau khi update
Write-Host "[3/3] Kiem tra phan bo staff sau khi cap nhat..." -ForegroundColor Cyan

function Run-LocalQuery([string]$sql) {
    $prevPgPass = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $dbPassword
        $res = & $psqlPath -h $dbHost -p $dbPort -U $dbUser -d $dbName -t -c $sql
        return ($res -join "`n").Trim()
    } finally {
        $env:PGPASSWORD = $prevPgPass
    }
}

$vetDist = Run-LocalQuery "
SELECT concat('    - ', u.email, ': ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.veterinarian_id = u.id
GROUP BY u.email ORDER BY count(a.id) DESC;
"

$trnDist = Run-LocalQuery "
SELECT concat('    - ', u.email, ' (', u.full_name, '): ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.trainer_id = u.id
GROUP BY u.email, u.full_name ORDER BY count(a.id) DESC;
"

$rraDist = Run-LocalQuery "
SELECT concat('    - ', u.email, ' (', u.full_name, '): ', count(r.id), ' assessments')
FROM racing_readiness_assessments r
JOIN users u ON r.trainer_id = u.id
GROUP BY u.email, u.full_name ORDER BY count(r.id) DESC;
"

$grmDist = Run-LocalQuery "
SELECT concat('    - ', u.email, ': ', count(a.id), ' admissions')
FROM admission_applications a
JOIN users u ON a.groom_id = u.id
GROUP BY u.email ORDER BY count(a.id) DESC LIMIT 5;
"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " REBALANCE STAFF LOCAL POSTGRESQL THANH CONG! " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Phan bo Veterinarian (Admissions):" -ForegroundColor White
Write-Host $vetDist -ForegroundColor Gray
Write-Host "Phan bo Trainer (Admissions):" -ForegroundColor White
Write-Host $trnDist -ForegroundColor Gray
Write-Host "Phan bo Trainer (Racing Readiness Assessments):" -ForegroundColor White
Write-Host $rraDist -ForegroundColor Gray
Write-Host "Phan bo Groom (Top 5):" -ForegroundColor White
Write-Host $grmDist -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Green
