<#
.SYNOPSIS
    Script tu dong tai va cai dat pgvector cho PostgreSQL tren Windows (PG 16, 17, 18).
#>

$ErrorActionPreference = 'Stop'

# Kiem tra quyen Administrator (de copy vao C:\Program Files)
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "Dang yeu cau quyen Administrator de copy file vao Program Files..." -ForegroundColor Yellow
    try {
        Start-Process powershell -Verb RunAs -ArgumentList "-ExecutionPolicy Bypass -NoExit -File `"$PSCommandPath`"" -ErrorAction Stop
        exit
    } catch {
        Write-Host ""
        Write-Host "[LỖI] Script can quyen Administrator de ghi vao thu muc C:\Program Files\PostgreSQL." -ForegroundColor Red
        Write-Host "Cach lam: Bam Start -> tim 'PowerShell' -> Chuot phai chon 'Run as Administrator' -> chay lai script:" -ForegroundColor Yellow
        Write-Host "cd d:\Workspace\racehorse-training-system" -ForegroundColor Gray
        Write-Host ".\scripts\setup-pgvector-windows.ps1" -ForegroundColor Gray
        exit 1
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "    RTMS - CAI DAT PGVECTOR CHO POSTGRESQL TREN WINDOWS   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Phat hien thu muc PostgreSQL
$pgBasePath = "E:\postgresql"
if (-not (Test-Path $pgBasePath)) {
    Write-Host "[ERROR] Khong tim thay thu muc: $pgBasePath" -ForegroundColor Red
    Write-Host "Vui long cai dat PostgreSQL tren may truoc khi chay script." -ForegroundColor Yellow
    exit 1
}

# Dam bao luon luon la Array du chi co 1 phien ban duy nhat
$pgVersions = @(Get-ChildItem -Path $pgBasePath -Directory | Where-Object { $_.Name -match '^\d+$' } | Sort-Object { [int]$_.Name } -Descending)

if ($pgVersions.Length -eq 0) {
    Write-Host "[ERROR] Khong tim thay phien ban PostgreSQL nao trong $pgBasePath!" -ForegroundColor Red
    exit 1
}

$selectedPg = $pgVersions[0]
$pgVersion = [int]$selectedPg.Name
$pgHome = $selectedPg.FullName

Write-Host "[1/4] Da phat hien PostgreSQL phien ban $pgVersion tai:" -ForegroundColor Green
Write-Host "      $pgHome" -ForegroundColor Gray

if ($pgVersion -lt 14 -or $pgVersion -gt 18) {
    Write-Host "[ERROR] Phien ban PostgreSQL $pgVersion chua duoc ho tro san goi binary." -ForegroundColor Red
    Write-Host "Khuyen nghi su dung PostgreSQL 16, 17 hoac 18." -ForegroundColor Yellow
    exit 1
}

# 2. Tai goi pgvector binary tuong ung
$zipName = "vector.v0.8.7-pg$pgVersion.zip"
$downloadUrl = "https://github.com/andreiramani/pgvector_pgsql_windows/releases/download/0.8.7/$zipName"
$tempDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), "rtms-pgvector-$pgVersion")

if (Test-Path $tempDir) {
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
$zipPath = Join-Path $tempDir $zipName

Write-Host "[2/4] Dang tai $zipName tu GitHub..." -ForegroundColor Yellow
try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing
    Write-Host "      Tai ve thanh cong!" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Khong the tai goi pgvector tu: $downloadUrl" -ForegroundColor Red
    Write-Host "Chi tiet: $_" -ForegroundColor Red
    exit 1
}

# 3. Giai nen va copy vao thu muc PostgreSQL
Write-Host "[3/4] Dang giai nen va sao chep extension vao PostgreSQL..." -ForegroundColor Yellow
$extractDir = Join-Path $tempDir "extracted"
Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force

# Copy DLL vao lib/
$libSource = Join-Path $extractDir "lib"
$libDest = Join-Path $pgHome "lib"
if (Test-Path $libSource) {
    Copy-Item -Path (Join-Path $libSource "*") -Destination $libDest -Force -Recurse
    Write-Host "      -> Da copy vector.dll vao $libDest" -ForegroundColor Gray
}

# Copy SQL & Control vao share/extension/
$shareSource = Join-Path $extractDir "share\extension"
$shareDest = Join-Path $pgHome "share\extension"
if (Test-Path $shareSource) {
    Copy-Item -Path (Join-Path $shareSource "*") -Destination $shareDest -Force -Recurse
    Write-Host "      -> Da copy control va sql files vao $shareDest" -ForegroundColor Gray
}

# Copy Headers vao include/
$includeSource = Join-Path $extractDir "include"
$includeDest = Join-Path $pgHome "include"
if (Test-Path $includeSource) {
    Copy-Item -Path (Join-Path $includeSource "*") -Destination $includeDest -Force -Recurse -ErrorAction SilentlyContinue
}

# Don dep thu muc tam
Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue

# 4. Khoi dong lai dich vu PostgreSQL
Write-Host "[4/4] Dang khoi dong lai dich vu PostgreSQL..." -ForegroundColor Yellow
$serviceName = "postgresql-x64-$pgVersion"
$pgService = Get-Service -Name $serviceName -ErrorAction SilentlyContinue

if ($pgService) {
    Restart-Service -Name $serviceName -Force
    Write-Host "      Dich vu $serviceName da duoc khoi dong lai thanh cong!" -ForegroundColor Green
} else {
    Write-Host "      [WARNING] Khong tim thay service $serviceName. Vui long restart PostgreSQL thu cong trong services.msc." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "           CAI DAT PGVECTOR HOAN TAT 100%!               " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Cac buoc tiep theo:" -ForegroundColor Cyan
Write-Host "1. Mo pgAdmin hoac psql (cong 5432, user postgres):" -ForegroundColor White
Write-Host "   CREATE DATABASE rtms_db;" -ForegroundColor Yellow
Write-Host "   CREATE EXTENSION IF NOT EXISTS vector;" -ForegroundColor Yellow
Write-Host ""
Write-Host "2. Copy file config backend:" -ForegroundColor White
Write-Host "   application-local.properties.example -> application-local.properties" -ForegroundColor Gray
Write-Host "   Dien mat khau postgres cua ban vao spring.datasource.password" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Chay backend:" -ForegroundColor White
Write-Host "   cd backend; .\mvnw spring-boot:run" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Cyan