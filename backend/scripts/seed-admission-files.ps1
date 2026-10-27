# seed-admission-files.ps1
# Chep file ho so nhap hoc (anh + PDF) tu bo ho so da duyet vao thu muc upload cua backend tren o dia.
# (Copies the admission documents (photos + PDFs) from the reviewed pack into the backend upload folder on disk.)
#
# - Tu tim thu muc RTMS_Admission_Packs_* nam trong docs/ hoac canh thu muc repo (moi nhat theo ten), hoac dung -Source.
# - Tinh dau van tay (SHA-256) cua toan bo file nguon. Neu trung voi lan chep truoc va du file dich -> bo qua.
# - Neu khac (nguon doi, thieu file dich) hoac co -Force -> XOA SACH thu muc upload roi chep lai, nen dung luong
#   thu muc luon bang dung bo nguon, khong bao gio nhan doi.
# - Ten file dich = local:<uuid>.<duoi>, uuid = md5('rtms-seed/' + duong dan trong bo ho so); seed database dung
#   dung cong thuc nay (pg_temp.file_key) nen hai script khop nhau.
#
# Cach dung (Usage), dung o thu muc backend:
#   .\scripts\seed-admission-files.ps1
#   .\scripts\seed-admission-files.ps1 -Source 'D:\path\RTMS_Admission_Packs_2026-10-17'
#   .\scripts\seed-admission-files.ps1 -Force
param(
    [string]$Source,
    [string]$UploadDir,
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $backendRoot '..'))

# 1. Thu muc nguon (Source folder)
if (-not $Source) { $Source = $env:RTMS_ADMISSION_PACKS }
if (-not $Source) {
    $searchDirs = @((Join-Path $repoRoot 'docs'), (Join-Path $repoRoot '..'))
    foreach ($dir in $searchDirs) {
        if (Test-Path -LiteralPath $dir) {
            $candidate = Get-ChildItem -LiteralPath $dir -Directory -Filter 'RTMS_Admission_Packs_*' |
                Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'upload-manifest.csv') } |
                Sort-Object Name | Select-Object -Last 1
            if ($candidate) {
                $Source = $candidate.FullName
                break
            }
        }
    }
    if (-not $Source) {
        throw "Admission pack not found. Put the RTMS_Admission_Packs_<date> folder inside docs/ or next to the repository folder, or pass -Source."
    }
}
$Source = (Resolve-Path -LiteralPath $Source).Path
$manifestPath = Join-Path $Source 'upload-manifest.csv'
if (-not (Test-Path -LiteralPath $manifestPath)) { throw "upload-manifest.csv is missing in $Source" }

# 2. Thu muc dich: giong rtms.storage.upload-dir cua backend
# Ho tro ca 2 vi tri (backend\uploads\admissions va root uploads\admissions khi chay qua IntelliJ IDEA)
$targetDirs = @()
if ($UploadDir) {
    if (-not [System.IO.Path]::IsPathRooted($UploadDir)) { $UploadDir = Join-Path $backendRoot $UploadDir }
    $targetDirs += $UploadDir
} else {
    $custom = $env:RTMS_STORAGE_UPLOAD_DIR
    if (-not $custom) { $custom = $env:RTMS_ADMISSION_UPLOAD_DIR }
    if ($custom) {
        if (-not [System.IO.Path]::IsPathRooted($custom)) { $custom = Join-Path $backendRoot $custom }
        $targetDirs += $custom
    } else {
        $targetDirs += (Join-Path $backendRoot 'uploads\admissions')
        $targetDirs += (Join-Path $repoRoot 'uploads\admissions')
    }
}

foreach ($dir in $targetDirs) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$primaryUploadDir = (Resolve-Path -LiteralPath $targetDirs[0]).Path
$statePath = Join-Path $primaryUploadDir '.rtms-seed-state.json'

Write-Host "Source : $Source" -ForegroundColor Cyan
Write-Host "Targets: $($targetDirs -join ', ')" -ForegroundColor Cyan

# 3. Danh sach file + dau van tay tu noi dung that (Files and a fingerprint of their real content)
$md5 = [System.Security.Cryptography.MD5]::Create()
$rows = Import-Csv -LiteralPath $manifestPath -Encoding UTF8
$files = @()
$mismatch = 0
$fingerprintLines = New-Object System.Collections.Generic.List[string]
foreach ($row in $rows) {
    $relative = $row.path
    $sourceFile = Join-Path $Source ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $sourceFile)) { throw "Missing source file: $sourceFile" }
    $hash = (Get-FileHash -LiteralPath $sourceFile -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($row.sha256 -and $hash -ne $row.sha256.ToLowerInvariant()) { $mismatch++ }
    $bytes = $md5.ComputeHash([System.Text.Encoding]::UTF8.GetBytes('rtms-seed/' + $relative))
    $hex = -join ($bytes | ForEach-Object { $_.ToString('x2') })
    $uuid = '{0}-{1}-{2}-{3}-{4}' -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20, 12)
    $name = $uuid + [System.IO.Path]::GetExtension($relative).ToLowerInvariant()
    $size = (Get-Item -LiteralPath $sourceFile).Length
    $files += [pscustomobject]@{ Source = $sourceFile; Name = $name; Size = $size }
    $fingerprintLines.Add("$relative|$hash|$size")
}
$joined = [System.Text.Encoding]::UTF8.GetBytes(($fingerprintLines -join "`n"))
$fingerprint = -join ([System.Security.Cryptography.SHA256]::Create().ComputeHash($joined) | ForEach-Object { $_.ToString('x2') })
if ($mismatch -gt 0) {
    Write-Host "Note: $mismatch file(s) differ from the SHA-256 listed in upload-manifest.csv (the pack was edited)." -ForegroundColor Yellow
}

# 4. Da chep dung bo nay roi thi bo qua (Skip when this exact pack is already in place across all targets)
$allUpToDate = $true
if (-not $Force) {
    foreach ($targetDir in $targetDirs) {
        $st = Join-Path $targetDir '.rtms-seed-state.json'
        if (-not (Test-Path -LiteralPath $st)) {
            $allUpToDate = $false
            break
        }
        $state = Get-Content -LiteralPath $st -Raw | ConvertFrom-Json
        if ($state.fingerprint -ne $fingerprint) {
            $allUpToDate = $false
            break
        }
        $missing = @($files | Where-Object {
            $target = Join-Path $targetDir $_.Name
            -not (Test-Path -LiteralPath $target) -or (Get-Item -LiteralPath $target).Length -ne $_.Size
        })
        if ($missing.Count -gt 0) {
            $allUpToDate = $false
            break
        }
    }
} else {
    $allUpToDate = $false
}

if ($allUpToDate) {
    Write-Host "Admission files are up to date ($($files.Count) files) across all targets. Nothing copied. Use -Force to copy again." -ForegroundColor Green
    exit 0
}

# 5. Chep vao tat ca thu muc dich (Wipe and copy into all target folders)
foreach ($targetDir in $targetDirs) {
    Write-Host "Syncing into $targetDir..." -ForegroundColor Cyan
    $old = @(Get-ChildItem -LiteralPath $targetDir -Force -File)
    if ($old.Count -gt 0) {
        $old | Remove-Item -Force
    }
    Get-ChildItem -LiteralPath $targetDir -Force -Directory | Remove-Item -Recurse -Force
    $i = 0
    foreach ($file in $files) {
        Copy-Item -LiteralPath $file.Source -Destination (Join-Path $targetDir $file.Name)
        $i++
        if ($i % 100 -eq 0) { Write-Host "  copied $i / $($files.Count)" }
    }
    [pscustomobject]@{
        fingerprint = $fingerprint
        source      = $Source
        copiedAt    = (Get-Date).ToString('s')
        fileCount   = $files.Count
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $targetDir '.rtms-seed-state.json') -Encoding UTF8
}

$totalMb = [math]::Round((($files | Measure-Object -Property Size -Sum).Sum) / 1MB, 1)
Write-Host "Copied $($files.Count) admission files ($totalMb MB) into $($targetDirs.Count) directory(ies)" -ForegroundColor Green
Write-Host 'Run .\scripts\seed-database.ps1 next if you have not seeded the database yet.' -ForegroundColor Green
